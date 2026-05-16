package dev.overgrown.apoli.condition.builtin.fluid;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.FluidCtx;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class EmptyFluidCondition implements ConditionType<FluidCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, FluidCtx ctx) {
        return ctx.state().isEmpty();
    }
}