package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ModifyFogHandler {

    private static final int START = 0;
    private static final int END = 1;
    private static final int RED = 2;
    private static final int GREEN = 3;
    private static final int BLUE = 4;
    private static final int FADE = 5;
    private static final int SLOTS = 6;

    private ModifyFogHandler() {}

    private static final class Fold {
        private final float[] values = new float[SLOTS];
        private final int[] priorities = new int[SLOTS];
        private final boolean[] claimed = new boolean[SLOTS];
        private float fadeIn;
        private float fadeOut;

        private void reset() {
            for (int i = 0; i < SLOTS; i++) claimed[i] = false;
            fadeIn = 0f;
            fadeOut = 0f;
        }

        private void offer(int slot, int priority, float value) {
            if (claimed[slot] && priority <= priorities[slot]) return;
            claimed[slot] = true;
            priorities[slot] = priority;
            values[slot] = value;
        }

        private float valueOr(int slot, float fallback) {
            return claimed[slot] ? values[slot] : fallback;
        }
    }

    private static final ThreadLocal<Fold> FOLD = ThreadLocal.withInitial(Fold::new);

    public static FogData getFog(@Nullable Entity entity) {
        Fold fold = FOLD.get();
        fold.reset();

        PowerLookup.forEach(entity, ApoliIds.MODIFY_FOG, ModifyFogPower.Config.class, cfg -> {
            int priority = cfg.priority();
            if (cfg.s().isPresent()) fold.offer(START, priority, (float) cfg.s().get().eval(entity));
            if (cfg.v().isPresent()) fold.offer(END, priority, (float) cfg.v().get().eval(entity));
            if (cfg.r().isPresent()) fold.offer(RED, priority, (float) cfg.r().get().eval(entity));
            if (cfg.g().isPresent()) fold.offer(GREEN, priority, (float) cfg.g().get().eval(entity));
            if (cfg.b().isPresent()) fold.offer(BLUE, priority, (float) cfg.b().get().eval(entity));
            if (!fold.claimed[FADE] || priority > fold.priorities[FADE]) {
                fold.offer(FADE, priority, 0f);
                fold.fadeIn = (float) cfg.fadeIn().eval(entity);
                fold.fadeOut = (float) cfg.fadeOut().eval(entity);
            }
        });

        float unset = FogData.UNSET;
        Vec3 color = fold.claimed[RED] || fold.claimed[GREEN] || fold.claimed[BLUE]
            ? new Vec3(fold.valueOr(RED, unset), fold.valueOr(GREEN, unset), fold.valueOr(BLUE, unset))
            : FogData.UNSET_COLOR;

        return new FogData(fold.valueOr(START, unset), fold.valueOr(END, unset), color,
            fold.fadeIn, fold.fadeOut);
    }

    public static final class FogData {
        public static final float UNSET = -1f;
        public static final Vec3 UNSET_COLOR = new Vec3(UNSET, UNSET, UNSET);

        public final float s;
        public final float v;
        public final Vec3 color;
        public final float fadeIn;
        public final float fadeOut;

        public FogData(float s, float v, Vec3 color, float fadeIn, float fadeOut) {
            this.s = s;
            this.v = v;
            this.color = color;
            this.fadeIn = fadeIn;
            this.fadeOut = fadeOut;
        }

        public static FogData empty() {
            return new FogData(UNSET, UNSET, UNSET_COLOR, 0f, 0f);
        }

        public boolean sameAs(FogData other) {
            return s == other.s && v == other.v && color.equals(other.color);
        }

        public FogData lerp(FogData target, float progress,
                            @Nullable Float defaultS, @Nullable Float defaultV, @Nullable Vec3 defaultColor) {
            float span = this.fadeOut + target.fadeIn;
            float percent = span <= 0f ? 1f : Math.min(1f, progress / span);
            if (percent >= 1f) return target;

            float fromS = s == UNSET ? Objects.requireNonNullElse(defaultS, target.s) : s;
            float fromV = v == UNSET ? Objects.requireNonNullElse(defaultV, target.v) : v;
            Vec3 fromColor = color.equals(UNSET_COLOR)
                ? Objects.requireNonNullElse(defaultColor, target.color) : color;

            float toS = target.s == UNSET ? Objects.requireNonNullElse(defaultS, fromS) : target.s;
            float toV = target.v == UNSET ? Objects.requireNonNullElse(defaultV, fromV) : target.v;
            Vec3 toColor = target.color.equals(UNSET_COLOR)
                ? Objects.requireNonNullElse(defaultColor, fromColor) : target.color;

            return new FogData(
                fromS + (toS - fromS) * percent,
                fromV + (toV - fromV) * percent,
                fromColor.add(toColor.subtract(fromColor).scale(percent)),
                target.fadeIn, target.fadeOut);
        }
    }
}
