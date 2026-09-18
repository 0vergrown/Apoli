package dev.overgrown.apoli.scale;

import org.jetbrains.annotations.Nullable;

public interface ScaleHolder {
    @Nullable
    ScaleState apoli$scales();

    ScaleState apoli$scalesOrCreate();
}
