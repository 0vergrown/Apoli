package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.data.ModelPartTimeline;
import dev.overgrown.apoli.data.ModelPartTransformation;
import dev.overgrown.apoli.mixin.modelparts.ModelPartCubesAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class ModelPartEdits {
    private ModelPartEdits() {}

    public static float[] snapshot(ModelPart part) {
        return new float[]{
            part.x, part.y, part.z,
            part.xRot, part.yRot, part.zRot,
            part.xScale, part.yScale, part.zScale,
            part.visible ? 1f : 0f, part.skipDraw ? 1f : 0f
        };
    }

    public static void restore(ModelPart part, float[] o) {
        restorePose(part, o);
        part.visible = o[9] != 0f;
        part.skipDraw = o[10] != 0f;
    }

    public static void restorePose(ModelPart part, float[] o) {
        part.x = o[0]; part.y = o[1]; part.z = o[2];
        part.xRot = o[3]; part.yRot = o[4]; part.zRot = o[5];
        part.xScale = o[6]; part.yScale = o[7]; part.zScale = o[8];
    }

    public static boolean endPoint(ModelPart part, float[] out) {
        List<ModelPart.Cube> cubes = ((ModelPartCubesAccessor) (Object) part).apoli$modelCubes();
        int n = cubes.size();
        if (n == 0) return false;
        float minX = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            ModelPart.Cube cube = cubes.get(i);
            minX = Math.min(minX, cube.minX);
            minZ = Math.min(minZ, cube.minZ);
            maxX = Math.max(maxX, cube.maxX);
            maxY = Math.max(maxY, cube.maxY);
            maxZ = Math.max(maxZ, cube.maxZ);
        }
        out[0] = (minX + maxX) * 0.5F;
        out[1] = maxY;
        out[2] = (minZ + maxZ) * 0.5F;
        return true;
    }

    public static boolean changesVisibility(ModelPartTransformation.Type type) {
        return type == ModelPartTransformation.Type.VISIBLE || type == ModelPartTransformation.Type.HIDDEN;
    }

    public static boolean hides(ModelPartTimeline.Slot slot) {
        if (slot.weight() < 0.5f) return false;
        ModelPartTransformation.Type type = slot.transformation().type();
        if (type == ModelPartTransformation.Type.VISIBLE) return slot.value() == 0f;
        return type == ModelPartTransformation.Type.HIDDEN && slot.value() != 0f;
    }

    public static void apply(ModelPart part, ModelPartTimeline.Slot slot, @Nullable float[] o) {
        ModelPartTransformation t = slot.transformation();
        float value = slot.value();
        float weight = slot.weight();
        boolean override = t.overrideAnimation();
        switch (t.type()) {
            case PITCH -> part.xRot = override ? part.xRot + (value - part.xRot) * weight : part.xRot + value * weight;
            case YAW -> part.yRot = override ? part.yRot + (value - part.yRot) * weight : part.yRot + value * weight;
            case ROLL -> part.zRot = override ? part.zRot + (value - part.zRot) * weight : part.zRot + value * weight;
            case VISIBLE -> { if (weight >= 0.5f) part.visible = value != 0f; }
            case HIDDEN -> { if (weight >= 0.5f) part.skipDraw = value != 0f; }
            case X_SCALE -> part.xScale = (o != null ? o[6] : 1f) + value * weight;
            case Y_SCALE -> part.yScale = (o != null ? o[7] : 1f) + value * weight;
            case Z_SCALE -> part.zScale = (o != null ? o[8] : 1f) + value * weight;
            case PIVOT_X -> part.x += value * weight;
            case PIVOT_Y -> part.y += value * weight;
            case PIVOT_Z -> part.z += value * weight;
        }
    }
}
