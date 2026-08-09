package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.builtin.damage.AttackChargeDamageCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.world.entity.player.Player;

public final class AttackChargeCondition implements ConditionType<EntityCtx, AttackChargeCondition.Cfg> {

    public record Cfg(Comparison comparison, Expression compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.LESS_EQUAL).forGetter(Cfg::comparison),
            Expression.FLOAT_OR_EXPR
                .optionalFieldOf("compare_to", Expression.constant(AttackChargeDamageCondition.FULL_CHARGE))
                .forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return false;
        float scale = player.getAttackStrengthScale(0.5f);
        return cfg.comparison.compare(scale, cfg.compareTo.eval(player, scale));
    }
}
