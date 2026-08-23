package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
        PIVOT_Z;

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

    public record Keyframe(float time, Expression value, Optional<Easing> easing) {
        public static final Codec<Keyframe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("time").forGetter(Keyframe::time),
            Expression.FLOAT_OR_EXPR.fieldOf("value").forGetter(Keyframe::value),
            Easing.CODEC.optionalFieldOf("easing").forGetter(Keyframe::easing)
        ).apply(instance, Keyframe::new));

        public float valueFor(@Nullable Entity entity) {
            return (float) value.eval(entity);
        }
    }

    private final String part;
    private final String normalizedPart;
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
    private final float timelineStart;
    private final float timelineEnd;

    public ModelPartTransformation(String part, Type type, Optional<Expression> value, boolean overrideAnimation,
                                   List<Keyframe> keyframes, boolean loop, float duration,
                                   Optional<Float> fadeOutDuration, Easing easing) {
        this.part = part;
        this.normalizedPart = ModelParts.normalize(part);
        this.type = type;
        this.rawValue = value;
        this.value = value.orElse(Expression.constant(0.0));
        this.overrideAnimation = overrideAnimation;
        this.keyframes = sortByTime(keyframes);
        this.loop = loop;
        this.duration = duration;
        this.rawFadeOutDuration = fadeOutDuration;
        this.fadeOutDuration = fadeOutDuration.orElse(duration);
        this.easing = easing;
        this.timelineStart = this.keyframes.isEmpty() ? 0.0F : this.keyframes.get(0).time();
        this.timelineEnd = this.keyframes.isEmpty() ? 0.0F : this.keyframes.get(this.keyframes.size() - 1).time();
    }

    private static List<Keyframe> sortByTime(List<Keyframe> keyframes) {
        if (keyframes.size() < 2) return keyframes;
        List<Keyframe> sorted = new ArrayList<>(keyframes);
        sorted.sort((a, b) -> Float.compare(a.time(), b.time()));
        return List.copyOf(sorted);
    }

    public String part() {
        return part;
    }

    public String normalizedPart() {
        return normalizedPart;
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

    public float sample(float elapsed, @Nullable Entity entity) {
        List<Keyframe> frames = keyframes;
        int size = frames.size();
        if (size == 0) return valueFor(entity);
        if (size == 1) return frames.get(0).valueFor(entity);

        float t = elapsed;
        float span = timelineEnd - timelineStart;
        if (loop && span > 0.0F) {
            t = timelineStart + (((t - timelineStart) % span) + span) % span;
        }
        if (t <= timelineStart) return frames.get(0).valueFor(entity);
        if (t >= timelineEnd) return frames.get(size - 1).valueFor(entity);

        int index = 1;
        while (index < size - 1 && frames.get(index).time() <= t) index++;

        Keyframe from = frames.get(index - 1);
        Keyframe to = frames.get(index);
        float segment = to.time() - from.time();
        float local = segment <= 0.0F ? 1.0F : (t - from.time()) / segment;
        Easing curve = to.easing().orElse(easing);
        float fromValue = from.valueFor(entity);
        float toValue = to.valueFor(entity);
        if (curve == Easing.CATMULLROM) {
            return catmullRom(valueAt(index - 2, entity), fromValue, toValue, valueAt(index + 1, entity), local);
        }
        return fromValue + (toValue - fromValue) * curve.apply(local);
    }

    private float valueAt(int index, @Nullable Entity entity) {
        int size = keyframes.size();
        int clamped = loop ? ((index % size) + size) % size : Math.max(0, Math.min(size - 1, index));
        return keyframes.get(clamped).valueFor(entity);
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
        if (rawValue.isEmpty() && keyframes.isEmpty()) {
            return DataResult.error(() -> "Model part transformation for '" + part + "' needs either a 'value' or a non-empty 'keyframes' list");
        }
        if (duration < 0.0F || fadeOutDuration < 0.0F) {
            return DataResult.error(() -> "Model part transformation for '" + part + "' has a negative duration");
        }
        return DataResult.success(this);
    }

    public static final Codec<ModelPartTransformation> CODEC = RecordCodecBuilder.<ModelPartTransformation>create(instance -> instance.group(
        Codec.STRING.fieldOf("model_part").forGetter(ModelPartTransformation::part),
        Type.CODEC.fieldOf("type").forGetter(ModelPartTransformation::type),
        Expression.FLOAT_OR_EXPR.optionalFieldOf("value").forGetter(ModelPartTransformation::rawValue),
        Codec.BOOL.optionalFieldOf("override_animation", false).forGetter(ModelPartTransformation::overrideAnimation),
        Keyframe.CODEC.listOf().optionalFieldOf("keyframes", List.of()).forGetter(ModelPartTransformation::keyframes),
        Codec.BOOL.optionalFieldOf("loop", false).forGetter(ModelPartTransformation::loop),
        Codec.FLOAT.optionalFieldOf("duration", 0.0F).forGetter(ModelPartTransformation::duration),
        Codec.FLOAT.optionalFieldOf("fade_out_duration").forGetter(ModelPartTransformation::rawFadeOutDuration),
        Easing.CODEC.optionalFieldOf("easing", Easing.LINEAR).forGetter(ModelPartTransformation::easing)
    ).apply(instance, ModelPartTransformation::new)).comapFlatMap(ModelPartTransformation::validate, Function.identity());
}
