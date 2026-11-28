package dev.overgrown.apoli.gamerule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BiomeCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.FluidCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.condition.context.StaticCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

// apoli:gamerule: tests a data pack gamerule (<namespace>:<name>) or a vanilla one (its plain name). Without
// comparison it passes when the rule is true or not zero. Registered for every condition context.
public final class GameRuleCondition<CTX> implements ConditionType<CTX, GameRuleCondition.Cfg> {
    // The rule id is parsed once at load, so a test only reads the value and compares.
    public record Cfg(String gamerule, Optional<Comparison> comparison, Optional<Double> compareTo,
                      @Nullable ResourceLocation id, @Nullable Comparison op, double target) {
        static Cfg of(String gamerule, Optional<Comparison> comparison, Optional<Double> compareTo) {
            ResourceLocation id = gamerule.indexOf(':') < 0 ? null : ResourceLocation.tryParse(gamerule);
            return new Cfg(gamerule, comparison, compareTo, id, comparison.orElse(null), compareTo.orElse(0.0));
        }

        int value(@Nullable Level level) {
            return id != null ? DataGameRules.value(id, level) : DataGameRules.vanillaValue(gamerule, level);
        }
    }

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
        Codec.STRING.fieldOf("gamerule").forGetter(Cfg::gamerule),
        Comparison.CODEC.optionalFieldOf("comparison").forGetter(Cfg::comparison),
        Codec.DOUBLE.optionalFieldOf("compare_to").forGetter(Cfg::compareTo)
    ).apply(i, Cfg::of)).flatXmap(GameRuleCondition::check, GameRuleCondition::check);

    private static DataResult<Cfg> check(Cfg cfg) {
        return cfg.comparison().isPresent() != cfg.compareTo().isPresent()
            ? DataResult.error(() -> "apoli:gamerule needs both \"comparison\" and \"compare_to\", or neither")
            : DataResult.success(cfg);
    }

    private final Function<CTX, Level> level;

    private GameRuleCondition(Function<CTX, Level> level) {
        this.level = level;
    }

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public boolean test(Cfg cfg, CTX ctx) {
        int value = cfg.value(level.apply(ctx));
        return cfg.op() == null ? value != 0 : cfg.op().compare(value, cfg.target());
    }

    static void register() {
        ResourceLocation id = Apoli.id("gamerule");
        ConditionTypes.ENTITY.register(id, new GameRuleCondition<EntityCtx>(EntityCtx::level));
        ConditionTypes.BI_ENTITY.register(id, new GameRuleCondition<BiEntityCtx>(BiEntityCtx::level));
        ConditionTypes.BLOCK.register(id, new GameRuleCondition<BlockCtx>(BlockCtx::level));
        ConditionTypes.ITEM.register(id, new GameRuleCondition<ItemCtx>(ItemCtx::level));
        ConditionTypes.DAMAGE.register(id, new GameRuleCondition<DamageCtx>(DamageCtx::level));
        ConditionTypes.FLUID.register(id, new GameRuleCondition<FluidCtx>(FluidCtx::level));
        ConditionTypes.BIOME.register(id, new GameRuleCondition<BiomeCtx>(BiomeCtx::level));
        // A static condition has no level of its own, so it reads the server's.
        ConditionTypes.STATIC.register(id, new GameRuleCondition<StaticCtx>(ctx -> DataGameRules.serverLevel()));
    }
}
