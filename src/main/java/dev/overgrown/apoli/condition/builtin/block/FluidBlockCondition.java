package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.FluidCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.FluidCtx;

public final class FluidBlockCondition implements ConditionType<BlockCtx, FluidBlockCondition.Cfg> {
    public record Cfg(FluidCondition fluidCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            FluidCondition.CODEC.fieldOf("fluid_condition").forGetter(Cfg::fluidCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        return cfg.fluidCondition.test(new FluidCtx(ctx.level().getFluidState(ctx.pos()), ctx.pos(), ctx.level()));
    }
}
