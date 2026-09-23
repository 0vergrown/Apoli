package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.scale.ScaleEasing;
import dev.overgrown.apoli.scale.ScaleOperation;
import dev.overgrown.apoli.scale.ScaleType;
import dev.overgrown.apoli.scale.ScaleTypeCodec;
import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.world.entity.Entity;

import java.util.List;

public final class ScaleAction implements ActionType<EntityCtx, ScaleAction.Cfg> {

    public record Cfg(List<ScaleType> types, ScaleOperation operation, Expression scale,
                      Expression ticks, ScaleEasing easing) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ScaleTypeCodec.LIST_OR_SINGLE.optionalFieldOf("scale_types", List.of(ScaleTypes.BASE)).forGetter(Cfg::types),
            ScaleOperation.CODEC.optionalFieldOf("operation", ScaleOperation.SET).forGetter(Cfg::operation),
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("scale", Expression.constant(1.0)).forGetter(Cfg::scale),
            Expression.INT_OR_EXPR.optionalFieldOf("ticks", Expression.constant(0.0)).forGetter(Cfg::ticks),
            ScaleEasing.CODEC.optionalFieldOf("easing", ScaleEasing.LINEAR).forGetter(Cfg::easing)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null || entity.level().isClientSide()) return;
        float argument = (float) cfg.scale.eval(entity);
        int ticks = Math.max(0, cfg.ticks.evalInt(entity));
        Scales.setAll(entity, cfg.types, cfg.operation, argument, ticks, cfg.easing);
    }
}
