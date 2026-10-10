package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.power.builtin.ModelColorPower;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class PartColorMap {

    private final Map<ModelPart, float[]> colors = new IdentityHashMap<>();
    private final List<float[]> pool = new ArrayList<>();
    private final List<ModelPart> parts = new ArrayList<>(8);
    private int used;

    PartColorMap() {}

    @Nullable
    Map<ModelPart, float[]> map() {
        return colors.isEmpty() ? null : colors;
    }

    void reset() {
        if (!colors.isEmpty()) colors.clear();
        used = 0;
    }

    void fill(Entity entity, HumanoidModel<?> model, List<ModelColorPower.PartColor> partColors,
              @Nullable ModelPart onlyA, @Nullable ModelPart onlyB) {
        for (int i = 0; i < partColors.size(); i++) {
            ModelColorPower.PartColor partColor = partColors.get(i);
            parts.clear();
            ModelPartLookup.resolveInto(model, partColor.part().sided(entity), parts);
            for (int p = 0; p < parts.size(); p++) {
                ModelPart part = parts.get(p);
                if (onlyA != null && part != onlyA && part != onlyB) continue;
                float[] color = entry(part);
                color[0] *= partColor.red();
                color[1] *= partColor.green();
                color[2] *= partColor.blue();
                color[3] *= partColor.alpha();
                if (partColor.whiten()) color[4] = 1f;
            }
        }
        parts.clear();
    }

    void multiply(ModelPart part, float[] whole) {
        float[] color = entry(part);
        color[0] *= whole[0];
        color[1] *= whole[1];
        color[2] *= whole[2];
        color[3] *= whole[3];
        color[4] = Math.max(color[4], whole[4]);
    }

    void attach(ModelPart part, float[] colours, int at) {
        System.arraycopy(colours, at, entry(part), 0, 5);
    }

    private float[] entry(ModelPart part) {
        float[] color = colors.get(part);
        if (color == null) {
            color = next();
            colors.put(part, color);
        }
        return color;
    }

    private float[] next() {
        float[] color;
        if (used < pool.size()) {
            color = pool.get(used);
        } else {
            color = new float[5];
            pool.add(color);
        }
        used++;
        color[0] = 1f;
        color[1] = 1f;
        color[2] = 1f;
        color[3] = 1f;
        color[4] = 0f;
        return color;
    }
}
