package dev.overgrown.apoli.power.builtin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ModifyFogInterpolator {
    private static ModifyFogHandler.FogData prev = ModifyFogHandler.FogData.empty();
    private static ModifyFogHandler.FogData target = ModifyFogHandler.FogData.empty();
    private static float progress = 0f;

    private static float default_s;
    private static float default_v;
    private static Vec3 default_color;

    public static void setDefaultS(float value) {
        default_s = value;
    }
    public static void setDefaultV(float value) {
        default_v = value;
    }
    public static void setDefaultColor(Vec3 value) {
        default_color = value;
    }

    private static void setTarget(ModifyFogHandler.FogData newTarget) {
        if (!newTarget.equals(target)) {
            prev = getCurrent(default_s, default_v, default_color);
            target = newTarget;
            progress = 0f;
        }
    }

    public static void tick(Entity entity) {
        progress += 0.05f;

        setTarget(ModifyFogHandler.getFog(entity));
    }

    public static ModifyFogHandler.FogData getCurrent(@Nullable Float default_s, @Nullable Float default_v, @Nullable Vec3 default_color) {
        return prev.lerp(target, progress, default_s, default_v, default_color);
    }
}
