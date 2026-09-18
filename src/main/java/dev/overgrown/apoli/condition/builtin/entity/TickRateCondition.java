package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.tick.TickRates;
import dev.overgrown.apoli.tick.TickScope;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class TickRateCondition implements ConditionType<EntityCtx, TickRateCondition.Cfg> {

    public record Cfg(TickScope scope, Optional<Comparison> comparison, Optional<Expression> compareTo,
                      Optional<Boolean> frozen) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TickScope.CODEC.optionalFieldOf("scope", TickScope.ENTITY).forGetter(Cfg::scope),
            Comparison.CODEC.optionalFieldOf("comparison").forGetter(Cfg::comparison),
            Expression.INT_OR_EXPR.optionalFieldOf("compare_to").forGetter(Cfg::compareTo),
            Codec.BOOL.optionalFieldOf("frozen").forGetter(Cfg::frozen)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null) return false;
        int rate = TickRates.effectiveRate(entity, cfg.scope);
        if (cfg.frozen.isPresent() && cfg.frozen.get() != (rate == 0)) return false;
        if (cfg.comparison.isEmpty()) return true;
        double compareTo = cfg.compareTo.map(expression -> expression.eval(entity)).orElse(0.0);
        return cfg.comparison.get().compare(rate, compareTo);
    }
}
