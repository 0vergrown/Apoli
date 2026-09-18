package dev.overgrown.apoli.scale;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ScaleOperation implements StringRepresentable {
    SET("set") {
        @Override
        public float apply(float current, float argument) {
            return argument;
        }
    },
    ADD("add") {
        @Override
        public float apply(float current, float argument) {
            return current + argument;
        }
    },
    SUBTRACT("subtract") {
        @Override
        public float apply(float current, float argument) {
            return current - argument;
        }
    },
    MULTIPLY("multiply") {
        @Override
        public float apply(float current, float argument) {
            return current * argument;
        }
    },
    DIVIDE("divide") {
        @Override
        public float apply(float current, float argument) {
            return argument == 0.0F ? current : current / argument;
        }
    },
    POWER("power") {
        @Override
        public float apply(float current, float argument) {
            return (float) Math.pow(current, argument);
        }
    };

    public static final Codec<ScaleOperation> CODEC = StringRepresentable.fromEnum(ScaleOperation::values);

    private final String name;

    ScaleOperation(String name) {
        this.name = name;
    }

    public abstract float apply(float current, float argument);

    @Override
    public String getSerializedName() {
        return name;
    }
}
