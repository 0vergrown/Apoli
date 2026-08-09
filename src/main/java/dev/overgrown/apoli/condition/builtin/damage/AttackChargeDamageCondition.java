package dev.overgrown.apoli.condition.builtin.damage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.data.AttackStrengthContext;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.world.entity.Entity;

public final class AttackChargeDamageCondition implements ConditionType<DamageCtx, AttackChargeDamageCondition.Cfg> {

    public static final float FULL_CHARGE = 0.9f;

    public record Cfg(Comparison comparison, Expression compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.LESS_EQUAL).forGetter(Cfg::comparison),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("compare_to", Expression.constant(FULL_CHARGE))
                .forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, DamageCtx ctx) {
        Entity attacker = ctx.source().getEntity();
        if (!AttackStrengthContext.has(attacker)) return false;
        float scale = AttackStrengthContext.scaleFor(attacker);
        return cfg.comparison.compare(scale, cfg.compareTo.eval(attacker, scale));
    }
}
