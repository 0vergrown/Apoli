package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ModifyFogHandler {
    static FogData getFog(Entity entity) {
        var result = FogData.empty();
        Map<Integer, Optional<Integer>> priorities = new HashMap<>(Map.of(0, Optional.empty(), 1, Optional.empty(), 2, Optional.empty(), 3, Optional.empty(), 4, Optional.empty(), 5, Optional.empty(), 6, Optional.empty()));

        PowerLookup.forEach(entity, Apoli.id("modify_fog"), ModifyFogPower.Config.class, cfg -> priorities.forEach((i, p) -> {
            if ((p.isEmpty() || cfg.priority() > p.get())) {
                switch (i) { //0 -> s, 1 -> v, 2 -> r, 3 -> g, 4 -> b, 5 -> fade_in, 6 -> fade_out
                    case 0:
                        if (cfg.s().isPresent()) result.s = (float) cfg.s().get().eval(entity);
                        break;
                    case 1:
                        if (cfg.v().isPresent()) result.v = (float) cfg.v().get().eval(entity);
                        break;
                    case 2:
                        if (cfg.r().isPresent()) result.color = new Vec3((float) cfg.r().get().eval(entity), result.color.y, result.color.z);
                        break;
                    case 3:
                        if (cfg.g().isPresent()) result.color = new Vec3(result.color.x, (float) cfg.g().get().eval(entity), result.color.z);
                        break;
                    case 4:
                        if (cfg.b().isPresent()) result.color = new Vec3(result.color.x, result.color.y, (float) cfg.b().get().eval(entity));
                        break;
                    case 5:
                        result.fade_in = (float) cfg.fade_in().eval(entity);
                        break;
                    case 6:
                        result.fade_out = (float) cfg.fade_out().eval(entity);
                        break;
                }
                priorities.replace(i, Optional.of(cfg.priority()));
            }
        }));

        return result;
    }

    public static final class FogData {
        public float s;
        public float v;

        public Vec3 color;

        float fade_in;
        float fade_out;

        public FogData(float s, float v, Vec3 color, float fade_in, float fade_out) {
            this.s = s;
            this.v = v;
            this.color = color;
            this.fade_in = fade_in;
            this.fade_out = fade_out;
        }

        public FogData lerp(FogData target, float progress, @Nullable Float default_s, @Nullable Float default_v, @Nullable Vec3 default_color) {
            var fade_duration = this.fade_out + target.fade_in;
            var percent = fade_duration <= 0 ? 1f : Math.min(1f, progress / fade_duration);

            var new_start = new FogData(s == -1f ? Objects.requireNonNullElse(default_s, target.s * 10) : s,
                    v == -1f ? Objects.requireNonNullElse(default_v, target.v * 10) : v,
                    color.equals(FogData.EMPTY.color) ? Objects.requireNonNullElse(default_color, Vec3.ZERO) : color,
                    target.fade_in, target.fade_out);
            var new_target = new FogData(target.s == -1f && percent != 1f ? Objects.requireNonNullElse(default_s, s * 10) : target.s,
                    target.v == -1f && percent != 1f ? Objects.requireNonNullElse(default_v, v * 10) : target.v,
                    target.color.equals(FogData.EMPTY.color) && percent != 1f ? Objects.requireNonNullElse(default_color, Vec3.ZERO) : target.color,
                    0f, 0f);


            FogData difference = new FogData((new_target.s - new_start.s) * percent, (new_target.v - new_start.v) * percent, new_target.color.subtract(new_start.color).scale(percent), 0, 0);

            return new_start.add(difference);
        }

        public FogData add(FogData f) {
            return new FogData(s + f.s, v + f.v, color.add(f.color), fade_in, fade_out);
        }

        public static final FogData EMPTY = FogData.empty();

        public static FogData empty() {
            return new FogData(-1f, -1f, new Vec3(-1f, -1f, -1f), 0f, 0f);
        }

        public boolean equals(FogData other) {
            return s == other.s && v == other.v && color.equals(other.color);
        }
    }
}
