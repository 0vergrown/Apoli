package dev.overgrown.apoli.compat.badoptimizations;

import dev.overgrown.apoli.client.render.LocalNightVision;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class BadOptimizationsCompat {

    private static float lastNightVision = Float.NaN;

    private BadOptimizationsCompat() {}

    public static boolean lightmapDirty() {
        float strength = LocalNightVision.strength(Minecraft.getInstance().player);
        if (strength == lastNightVision) return false;
        lastNightVision = strength;
        return true;
    }
}
