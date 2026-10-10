package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class ModelPartTransformation {

    public enum Type {
        PITCH,
        YAW,
        ROLL,
        VISIBLE,
        HIDDEN,
        X_SCALE,
        Y_SCALE,
        Z_SCALE,
        PIVOT_X,
        PIVOT_Y,
        PIVOT_Z,
        REACH;

        private static final Map<String, Type> BY_NAME = new HashMap<>();

        static {
            for (Type type : values()) {
                BY_NAME.put(ModelParts.normalize(type.name()), type);
            }
        }

        public static final Codec<Type> CODEC = Codec.STRING.comapFlatMap(
            string -> {
                Type type = BY_NAME.get(ModelParts.normalize(string));
                return type != null
                    ? DataResult.success(type)
                    : DataResult.error(() -> "Unknown model part transformation type: '" + string + "'");
            },
            type -> type.name().toLowerCase(Locale.ROOT)
        );
    }

    public record Keyframe(float time, Optional<Expression> value, Optional<Vector> target, Optional<Easing> easing) {
        public static final Codec<Keyframe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("time").forGetter(Keyframe::time),
            LoggedOptionalField.strict("value", Expression.FLOAT_OR_EXPR).forGetter(Keyframe::value),
            LoggedOptionalField.strict("target", Vector.CODEC).forGetter(Keyframe::target),
            Easing.CODEC.optionalFieldOf("easing").forGetter(Keyframe::easing)
        ).apply(instance, Keyframe::new));
    }

    private static final Expression FULL_REACH = Expression.constant(1.0);

    private final BodyPart bodyPart;
    private final Optional<Vector> pivot;
    private final Optional<Vector> target;
    private final Type type;
    private final Optional<Expression> rawValue;
    private final Expression value;
    private final boolean overrideAnimation;
    private final List<Keyframe> keyframes;
    private final boolean loop;
    private final float duration;
    private final Optional<Float> rawFadeOutDuration;
    private final float fadeOutDuration;
    private final Easing easing;
    private final Optional<List<Perspective>> perspectives;
    private final int perspectiveMask;
    private final float timelineStart;
    private final float timelineEnd;
    private final float[] valueTimes;
    private final Expression[] values;
    private final Easing[] valueEasings;
    private final float[] targetTimes;
    private final float[] targets;
    private final Easing[] targetEasings;

    public ModelPartTransformation(BodyPart bodyPart, Type type, Optional<Expression> value, boolean overrideAnimation,
                                   List<Keyframe> keyframes, boolean loop, float duration,
                                   Optional<Float> fadeOutDuration, Easing easing,
                                   Optional<List<Perspective>> perspectives, Optional<Vector> pivot,
                                   Optional<Vector> target) {
        this.bodyPart = bodyPart;
        this.pivot = pivot;
        this.target = target;
        this.type = type;
        this.rawValue = value;
        this.value = value.orElse(type == Type.REACH ? FULL_REACH : Expression.constant(0.0));
        this.overrideAnimation = overrideAnimation;
        this.keyframes = sortByTime(keyframes);
        this.loop = loop;
        this.duration = duration;
        this.rawFadeOutDuration = fadeOutDuration;
        this.fadeOutDuration = fadeOutDuration.orElse(duration);
        this.easing = easing;
        this.perspectives = perspectives;
        this.perspectiveMask = perspectives.map(Perspective::maskOf).orElse(Perspective.MASK_INHERIT);
        this.timelineStart = this.keyframes.isEmpty() ? 0.0F : this.keyframes.get(0).time();
        this.timelineEnd = this.keyframes.isEmpty() ? 0.0F : this.keyframes.get(this.keyframes.size() - 1).time();

        int valueCount = 0;
        int targetCount = 0;
        for (Keyframe keyframe : this.keyframes) {
            if (keyframe.value().isPresent()) valueCount++;
            if (keyframe.target().isPresent()) targetCount++;
        }
        boolean fixedTarget = targetCount == 0 && target.isPresent();
        this.valueTimes = new float[valueCount];
        this.values = new Expression[valueCount];
        this.valueEasings = new Easing[valueCount];
        this.targetTimes = new float[fixedTarget ? 1 : targetCount];
        this.targets = new float[this.targetTimes.length * 3];
        this.targetEasings = new Easing[this.targetTimes.length];
        int v = 0;
        int t = 0;
        for (Keyframe keyframe : this.keyframes) {
            Easing curve = keyframe.easing().orElse(easing);
            if (keyframe.value().isPresent()) {
                this.valueTimes[v] = keyframe.time();
                this.values[v] = keyframe.value().get();
                this.valueEasings[v] = curve;
                v++;
            }
            if (keyframe.target().isPresent()) {
                Vector point = keyframe.target().get();
                this.targetTimes[t] = keyframe.time();
                this.targets[t * 3] = point.x();
                this.targets[t * 3 + 1] = point.y();
                this.targets[t * 3 + 2] = point.z();
                this.targetEasings[t] = curve;
                t++;
            }
        }
        if (fixedTarget) {
            Vector point = target.get();
            this.targets[0] = point.x();
            this.targets[1] = point.y();
            this.targets[2] = point.z();
            this.targetEasings[0] = easing;
        }
    }

    private static List<Keyframe> sortByTime(List<Keyframe> keyframes) {
        if (keyframes.size() < 2) return keyframes;
        List<Keyframe> sorted = new ArrayList<>(keyframes);
        sorted.sort((a, b) -> Float.compare(a.time(), b.time()));
        return List.copyOf(sorted);
    }

    public BodyPart bodyPart() {
        return bodyPart;
    }

    public Optional<Vector> pivot() {
        return pivot;
    }

    public Optional<Vector> target() {
        return target;
    }

    public Type type() {
        return type;
    }

    public Optional<Expression> rawValue() {
        return rawValue;
    }

    public Expression value() {
        return value;
    }

    public float valueFor(@Nullable Entity entity) {
        return (float) value.eval(entity);
    }

    public boolean overrideAnimation() {
        return overrideAnimation;
    }

    public List<Keyframe> keyframes() {
        return keyframes;
    }

    public boolean loop() {
        return loop;
    }

    public float duration() {
        return duration;
    }

    public Optional<Float> rawFadeOutDuration() {
        return rawFadeOutDuration;
    }

    public float fadeOutDuration() {
        return fadeOutDuration;
    }

    public Easing easing() {
        return easing;
    }

    public Optional<List<Perspective>> perspectives() {
        return perspectives;
    }

    public int perspectiveMask() {
        return perspectiveMask;
    }

    public float sample(float elapsed, @Nullable Entity entity) {
        int size = values.length;
        if (size == 0) return valueFor(entity);
        if (size == 1) return valueAt(0, entity);

        float t = localTime(elapsed);
        float[] times = valueTimes;
        if (t <= times[0]) return valueAt(0, entity);
        if (t >= times[size - 1]) return valueAt(size - 1, entity);

        int index = segment(times, t);
        float local = progress(times, index, t);
        Easing curve = valueEasings[index];
        float fromValue = valueAt(index - 1, entity);
        float toValue = valueAt(index, entity);
        if (curve == Easing.CATMULLROM) {
            return catmullRom(valueAt(wrap(index - 2, size), entity), fromValue, toValue,
                valueAt(wrap(index + 1, size), entity), local);
        }
        return fromValue + (toValue - fromValue) * curve.apply(local);
    }

    public boolean sampleTarget(float elapsed, float[] out) {
        int size = targetTimes.length;
        if (size == 0) return false;
        float[] points = targets;
        if (size == 1) {
            copyTarget(0, out);
            return true;
        }

        float t = localTime(elapsed);
        float[] times = targetTimes;
        if (t <= times[0]) {
            copyTarget(0, out);
            return true;
        }
        if (t >= times[size - 1]) {
            copyTarget(size - 1, out);
            return true;
        }

        int index = segment(times, t);
        float local = progress(times, index, t);
        Easing curve = targetEasings[index];
        int from = (index - 1) * 3;
        int to = index * 3;
        if (curve == Easing.CATMULLROM) {
            int before = wrap(index - 2, size) * 3;
            int after = wrap(index + 1, size) * 3;
            for (int axis = 0; axis < 3; axis++) {
                out[axis] = catmullRom(points[before + axis], points[from + axis], points[to + axis],
                    points[after + axis], local);
            }
            return true;
        }
        float eased = curve.apply(local);
        for (int axis = 0; axis < 3; axis++) {
            out[axis] = points[from + axis] + (points[to + axis] - points[from + axis]) * eased;
        }
        return true;
    }

    private void copyTarget(int index, float[] out) {
        out[0] = targets[index * 3];
        out[1] = targets[index * 3 + 1];
        out[2] = targets[index * 3 + 2];
    }

    private float valueAt(int index, @Nullable Entity entity) {
        return (float) values[index].eval(entity);
    }

    private float localTime(float elapsed) {
        float span = timelineEnd - timelineStart;
        if (loop && span > 0.0F) {
            return timelineStart + (((elapsed - timelineStart) % span) + span) % span;
        }
        return elapsed;
    }

    private static int segment(float[] times, float t) {
        int index = 1;
        while (index < times.length - 1 && times[index] <= t) index++;
        return index;
    }

    private static float progress(float[] times, int index, float t) {
        float segment = times[index] - times[index - 1];
        return segment <= 0.0F ? 1.0F : (t - times[index - 1]) / segment;
    }

    private int wrap(int index, int size) {
        return loop ? ((index % size) + size) % size : Math.max(0, Math.min(size - 1, index));
    }

    private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5F * (2.0F * p1
            + (p2 - p0) * t
            + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2
            + (3.0F * p1 - p0 - 3.0F * p2 + p3) * t3);
    }

    private DataResult<ModelPartTransformation> validate() {
        if (type == Type.REACH && targetTimes.length == 0) {
            return DataResult.error(() -> "Model part transformation for '" + bodyPart + "' has type 'reach' but no point to reach: set 'target', or give its keyframes a 'target'");
        }
        if (rawValue.isEmpty() && keyframes.isEmpty() && type != Type.REACH) {
            return DataResult.error(() -> "Model part transformation for '" + bodyPart + "' needs either a 'value' or a non-empty 'keyframes' list");
        }
        for (Keyframe keyframe : keyframes) {
            if (keyframe.value().isPresent()) continue;
            if (type != Type.REACH) {
                return DataResult.error(() -> "Model part transformation for '" + bodyPart + "' has a keyframe at time " + keyframe.time() + " with no 'value'");
            }
            if (keyframe.target().isEmpty()) {
                return DataResult.error(() -> "Model part transformation for '" + bodyPart + "' has a keyframe at time " + keyframe.time() + " with neither a 'value' nor a 'target'");
            }
        }
        if (duration < 0.0F || fadeOutDuration < 0.0F) {
            return DataResult.error(() -> "Model part transformation for '" + bodyPart + "' has a negative duration");
        }
        return DataResult.success(this);
    }

    public static final Codec<ModelPartTransformation> CODEC = RecordCodecBuilder.<ModelPartTransformation>create(instance -> instance.group(
        BodyPart.CODEC.fieldOf("model_part").forGetter(ModelPartTransformation::bodyPart),
        Type.CODEC.fieldOf("type").forGetter(ModelPartTransformation::type),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("value").forGetter(ModelPartTransformation::rawValue),
        Codec.BOOL.optionalFieldOf("override_animation", false).forGetter(ModelPartTransformation::overrideAnimation),
        Keyframe.CODEC.listOf().optionalFieldOf("keyframes", List.of()).forGetter(ModelPartTransformation::keyframes),
        Codec.BOOL.optionalFieldOf("loop", false).forGetter(ModelPartTransformation::loop),
        Codec.FLOAT.optionalFieldOf("duration", 0.0F).forGetter(ModelPartTransformation::duration),
        Codec.FLOAT.optionalFieldOf("fade_out_duration").forGetter(ModelPartTransformation::rawFadeOutDuration),
        Easing.CODEC.optionalFieldOf("easing", Easing.LINEAR).forGetter(ModelPartTransformation::easing),
        Perspective.LIST_CODEC.optionalFieldOf("perspectives").forGetter(ModelPartTransformation::perspectives),
        LoggedOptionalField.of("pivot", Vector.CODEC).forGetter(ModelPartTransformation::pivot),
        LoggedOptionalField.of("target", Vector.CODEC).forGetter(ModelPartTransformation::target)
    ).apply(instance, ModelPartTransformation::new)).comapFlatMap(ModelPartTransformation::validate, Function.identity());
}
