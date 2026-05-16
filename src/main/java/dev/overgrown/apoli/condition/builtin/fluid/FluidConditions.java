package dev.overgrown.apoli.condition.builtin.fluid;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.ConditionTypes;

public final class FluidConditions {
    private FluidConditions() {}

    public static void register() {
        ConditionTypes.FLUID.register(Apoli.id("empty"), new EmptyFluidCondition());
        ConditionTypes.FLUID.register(Apoli.id("still"), new StillFluidCondition());
        ConditionTypes.FLUID.register(Apoli.id("in_tag"), new InTagFluidCondition());
    }
}