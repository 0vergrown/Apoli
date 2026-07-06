package dev.overgrown.apoli.client.render;

import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class ModelColorState {
    private ModelColorState() {}

    private static final ThreadLocal<Map<ModelPart, float[]>> PART_COLORS = new ThreadLocal<>();
    private static volatile boolean active;

    public static void set(@Nullable Map<ModelPart, float[]> map) {
        PART_COLORS.set(map);
        active = map != null && !map.isEmpty();
    }

    public static void clear() {
        PART_COLORS.remove();
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    @Nullable
    public static float[] colorFor(ModelPart part) {
        Map<ModelPart, float[]> map = PART_COLORS.get();
        return map == null ? null : map.get(part);
    }
}
