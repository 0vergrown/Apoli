package dev.overgrown.apoli.compat.badoptimizations;

import java.util.function.BooleanSupplier;

public final class BadOptimizationsLightmapHook implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        return BadOptimizationsCompat.lightmapDirty();
    }
}
