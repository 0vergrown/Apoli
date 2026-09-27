package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class EntitySetMembershipCondition implements ConditionType<EntityCtx, EntitySetMembershipCondition.Cfg> {
    public record Cfg(Optional<ResourceLocation> set, Comparison comparison, Expression compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("set", IdCodecs.ID).forGetter(Cfg::set),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison),
            Expression.INT_OR_EXPR.optionalFieldOf("compare_to", Expression.constant(1)).forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (ctx.level().isClientSide()) return false;
        int count = EntitySetPower.membershipCount(ctx.entity(), cfg.set.orElse(null));
        return cfg.comparison.compare(count, cfg.compareTo.evalInt(ctx.entity()));
    }
}
