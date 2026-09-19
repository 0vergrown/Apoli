package dev.overgrown.apoli.power.builtin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ModifyFogInterpolator {

    private static final float SECONDS_PER_TICK = 0.05f;
    private static final float MAX_PROGRESS = 1_000f;

    private static ModifyFogHandler.FogData previous = ModifyFogHandler.FogData.empty();
    private static ModifyFogHandler.FogData target = ModifyFogHandler.FogData.empty();
    private static float progress;

    private static @Nullable Level boundLevel;
    private static @Nullable Float defaultStart;
    private static @Nullable Float defaultEnd;
    private static @Nullable Vec3 defaultColor;

    private ModifyFogInterpolator() {}

    public static void setDefaultS(float value) {
        defaultStart = value;
    }

    public static void setDefaultV(float value) {
        defaultEnd = value;
    }

    public static void setDefaultColor(Vec3 value) {
        defaultColor = value;
    }

    public static void reset() {
        previous = ModifyFogHandler.FogData.empty();
        target = ModifyFogHandler.FogData.empty();
        progress = 0f;
        boundLevel = null;
        defaultStart = null;
        defaultEnd = null;
        defaultColor = null;
    }

    public static void tick(@Nullable Entity entity) {
        if (entity == null) {
            if (boundLevel != null) reset();
            return;
        }
        if (entity.level() != boundLevel) {
            reset();
            boundLevel = entity.level();
        }
        if (progress < MAX_PROGRESS) progress += SECONDS_PER_TICK;
        setTarget(ModifyFogHandler.getFog(entity));
    }

    private static void setTarget(ModifyFogHandler.FogData incoming) {
        if (incoming.sameAs(target)) return;
        previous = getCurrent(defaultStart, defaultEnd, defaultColor);
        target = incoming;
        progress = 0f;
    }

    public static ModifyFogHandler.FogData getCurrent(@Nullable Float defaultS, @Nullable Float defaultV,
                                                      @Nullable Vec3 defaultColorValue) {
        return previous.lerp(target, progress, defaultS, defaultV, defaultColorValue);
    }
}
