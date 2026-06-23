package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ReplaceLootTablePower extends PowerType<ReplaceLootTablePower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("replace_loot_table");

    public record Config(
        Map<String, ResourceLocation> replace,
        Optional<BiEntityCondition> bientityCondition,
        Optional<BlockCondition> blockCondition,
        Optional<ItemCondition> itemCondition,
        int priority,
        Map<Pattern, ResourceLocation> compiled
    ) {
        public Config(Map<String, ResourceLocation> r,
                      Optional<BiEntityCondition> bc,
                      Optional<BlockCondition> blc,
                      Optional<ItemCondition> ic,
                      int p) {
            this(r, bc, blc, ic, p, compile(r));
        }

        private static Map<Pattern, ResourceLocation> compile(Map<String, ResourceLocation> raw) {
            Map<Pattern, ResourceLocation> out = new HashMap<>(raw.size());
            for (Map.Entry<String, ResourceLocation> e : raw.entrySet()) {
                try { out.put(Pattern.compile(e.getKey()), e.getValue()); }
                catch (Exception ignored) {}
            }
            return Map.copyOf(out);
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).fieldOf("replace").forGetter(Config::replace),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }

    public static @Nullable ResourceLocation resolve(LivingEntity entity, ResourceLocation original) {
        String s = original.toString();
        ResourceLocation[] best = new ResourceLocation[]{null};
        int[] bestPriority = new int[]{Integer.MIN_VALUE};
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            for (Map.Entry<Pattern, ResourceLocation> e : cfg.compiled.entrySet()) {
                if (e.getKey().matcher(s).matches() && cfg.priority > bestPriority[0]) {
                    best[0] = e.getValue();
                    bestPriority[0] = cfg.priority;
                    return;
                }
            }
        });
        return best[0];
    }
}
