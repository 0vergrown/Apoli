package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public record ModelPartTransformation(String part, ModelPartTransformation.Type type, float value, boolean overrideAnimation) {

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

    public static final Codec<ModelPartTransformation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("model_part").forGetter(ModelPartTransformation::part),
        Type.CODEC.fieldOf("type").forGetter(ModelPartTransformation::type),
        Codec.FLOAT.fieldOf("value").forGetter(ModelPartTransformation::value),
        Codec.BOOL.optionalFieldOf("override_animation", false).forGetter(ModelPartTransformation::overrideAnimation)
    ).apply(instance, ModelPartTransformation::new));
}
