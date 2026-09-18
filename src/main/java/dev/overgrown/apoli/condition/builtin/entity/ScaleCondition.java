package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.scale.ScaleType;
import dev.overgrown.apoli.scale.ScaleTypeCodec;
import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.world.entity.Entity;

public final class ScaleCondition implements ConditionType<EntityCtx, ScaleCondition.Cfg> {

    public record Cfg(ScaleType type, Comparison comparison, Expression compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ScaleTypeCodec.CODEC.optionalFieldOf("scale_type", ScaleTypes.BASE).forGetter(Cfg::type),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Expression.DOUBLE_OR_EXPR.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null) return false;
        return cfg.comparison.compare(Scales.value(entity, cfg.type), cfg.compareTo.eval(entity));
    }
}
