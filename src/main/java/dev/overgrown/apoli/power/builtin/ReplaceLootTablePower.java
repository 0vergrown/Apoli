package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeUsage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ReplaceLootTablePower extends PowerType<ReplaceLootTablePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("replace_loot_table");
    public static final ResourceLocation REPLACED_TABLE = Apoli.id("replaced_loot_table");
    public static final PowerTypeUsage.Handle HELD = PowerTypeUsage.handle(CANONICAL);

    private static final Comparator<Config> BY_PRIORITY = Comparator.comparingInt(Config::priority);
    private static final ThreadLocal<List<Config>> CONFIGS = ThreadLocal.withInitial(() -> new ArrayList<>(2));
    private static final Set<ResourceLocation> WARNED = ConcurrentHashMap.newKeySet();
    private static final Codec<String> PATTERN = Codec.STRING.comapFlatMap(ReplaceLootTablePower::validPattern, pattern -> pattern);

    public record Replacement(Pattern pattern, String literal, ResourceLocation table) {
        public boolean matches(String id) {
            return this.literal.equals(id) || this.pattern.matcher(id).matches();
        }
    }

    public record Config(
        Map<String, ResourceLocation> replace,
        Optional<BiEntityCondition> bientityCondition,
        Optional<BlockCondition> blockCondition,
        Optional<ItemCondition> itemCondition,
        int priority,
        List<Replacement> replacements
    ) {
        public Config(Map<String, ResourceLocation> replace,
                      Optional<BiEntityCondition> bientityCondition,
                      Optional<BlockCondition> blockCondition,
                      Optional<ItemCondition> itemCondition,
                      int priority) {
            this(replace, bientityCondition, blockCondition, itemCondition, priority, compile(replace));
        }

        private static List<Replacement> compile(Map<String, ResourceLocation> raw) {
            List<Replacement> out = new ArrayList<>(raw.size());
            raw.forEach((regex, table) -> out.add(new Replacement(Pattern.compile(regex), regex, table)));
            return List.copyOf(out);
        }

        @Nullable
        public ResourceLocation replacementFor(String tableId) {
            for (int i = 0; i < this.replacements.size(); i++) {
                Replacement replacement = this.replacements.get(i);
                if (replacement.matches(tableId)) return replacement.table();
            }
            return null;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(PATTERN, IdCodecs.ID).fieldOf("replace").forGetter(Config::replace),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }

    private static DataResult<String> validPattern(String regex) {
        try {
            Pattern.compile(regex);
            return DataResult.success(regex);
        } catch (PatternSyntaxException e) {
            return DataResult.error(() -> "Invalid regular expression \"" + regex + "\": " + e.getDescription());
        }
    }

    public static void replacementsFor(LootContext context, String tableId, List<ResourceLocation> out) {
        Entity target = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        Entity holder = holderOf(context, target);
        if (holder == null) return;
        List<Config> configs = CONFIGS.get();
        configs.clear();
        PowerLookup.collect(holder, CANONICAL, Config.class, configs);
        if (configs.isEmpty()) return;
        if (configs.size() > 1) configs.sort(BY_PRIORITY);
        ServerLevel level = context.getLevel();
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        BlockPos pos = origin == null ? null : BlockPos.containing(origin);
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        for (int i = 0; i < configs.size(); i++) {
            Config cfg = configs.get(i);
            ResourceLocation table = cfg.replacementFor(tableId);
            if (table != null && applies(cfg, holder, target, level, tool == null ? ItemStack.EMPTY : tool, pos, state)) {
                out.add(table);
            }
        }
        configs.clear();
    }

    public static void warnMissing(ResourceLocation table) {
        if (WARNED.add(table)) {
            Apoli.LOGGER.warn("[Apoli] apoli:replace_loot_table points at loot table \"{}\", which does not exist; it is skipped.", table);
        }
    }

    @Nullable
    private static Entity holderOf(LootContext context, @Nullable Entity target) {
        if (target instanceof FishingHook hook) return hook.getOwner();
        DamageSource source = context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
        if (source != null) return source.getEntity() != null ? source.getEntity() : target;
        if (target instanceof Piglin piglin) {
            Optional<Player> player = piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
            return player.isPresent() ? player.get() : piglin;
        }
        return target;
    }

    private static boolean applies(Config cfg, Entity holder, @Nullable Entity target, ServerLevel level, ItemStack tool,
                                   @Nullable BlockPos pos, @Nullable BlockState state) {
        if (cfg.itemCondition().isPresent()
            && !cfg.itemCondition().get().test(new ItemCtx(tool, level, holder instanceof LivingEntity living ? living : null))) {
            return false;
        }
        if (cfg.blockCondition().isPresent()) {
            if (pos == null) return false;
            BlockState blockState = state != null ? state : level.getBlockState(pos);
            if (!cfg.blockCondition().get().test(new BlockCtx(pos, blockState, level, holder))) return false;
        }
        return cfg.bientityCondition().isEmpty() || cfg.bientityCondition().get().test(BiEntityCtx.of(holder, target, level));
    }
}
