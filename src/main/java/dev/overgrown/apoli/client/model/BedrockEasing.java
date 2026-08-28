package dev.overgrown.apoli.client.model;

import java.util.Locale;

public enum BedrockEasing {
    LINEAR("linear"),
    NONE("none"),
    STEP("step"),
    IN_SINE("easeinsine"),
    OUT_SINE("easeoutsine"),
    IN_OUT_SINE("easeinoutsine"),
    IN_QUAD("easeinquad"),
    OUT_QUAD("easeoutquad"),
    IN_OUT_QUAD("easeinoutquad"),
    IN_CUBIC("easeincubic"),
    OUT_CUBIC("easeoutcubic"),
    IN_OUT_CUBIC("easeinoutcubic"),
    IN_QUART("easeinquart"),
    OUT_QUART("easeoutquart"),
    IN_OUT_QUART("easeinoutquart"),
    IN_QUINT("easeinquint"),
    OUT_QUINT("easeoutquint"),
    IN_OUT_QUINT("easeinoutquint"),
    IN_EXPO("easeinexpo"),
    OUT_EXPO("easeoutexpo"),
    IN_OUT_EXPO("easeinoutexpo"),
    IN_CIRC("easeincirc"),
    OUT_CIRC("easeoutcirc"),
    IN_OUT_CIRC("easeinoutcirc"),
    IN_BACK("easeinback"),
    OUT_BACK("easeoutback"),
    IN_OUT_BACK("easeinoutback"),
    IN_ELASTIC("easeinelastic"),
    OUT_ELASTIC("easeoutelastic"),
    IN_OUT_ELASTIC("easeinoutelastic"),
    IN_BOUNCE("easeinbounce"),
    OUT_BOUNCE("easeoutbounce"),
    IN_OUT_BOUNCE("easeinoutbounce"),
    CATMULLROM("catmullrom");

    private static final BedrockEasing[] VALUES = values();
    private static final float BACK_SCALE = 1.70158F;

    private final String id;

    BedrockEasing(String id) {
        this.id = id;
    }

    public static BedrockEasing byIndex(int index) {
        return VALUES[index];
    }

    public static BedrockEasing byName(String raw) {
        String name = raw.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i].id.equals(name)) return VALUES[i];
        }
        return LINEAR;
    }

    public float ease(float t, float arg) {
        return switch (this) {
            case LINEAR, NONE, CATMULLROM -> t;
            case STEP -> t > 0.0F ? 1.0F : 0.0F;
            case IN_SINE -> sine(t);
            case OUT_SINE -> out(sine(1.0F - t));
            case IN_OUT_SINE -> inOut(t, arg);
            case IN_QUAD -> t * t;
            case OUT_QUAD -> out((1.0F - t) * (1.0F - t));
            case IN_OUT_QUAD -> inOut(t, arg);
            case IN_CUBIC -> t * t * t;
            case OUT_CUBIC -> out(cube(1.0F - t));
            case IN_OUT_CUBIC -> inOut(t, arg);
            case IN_QUART -> pow(t, 4);
            case OUT_QUART -> out(pow(1.0F - t, 4));
            case IN_OUT_QUART -> inOut(t, arg);
            case IN_QUINT -> pow(t, 5);
            case OUT_QUINT -> out(pow(1.0F - t, 5));
            case IN_OUT_QUINT -> inOut(t, arg);
            case IN_EXPO -> expo(t);
            case OUT_EXPO -> out(expo(1.0F - t));
            case IN_OUT_EXPO -> inOut(t, arg);
            case IN_CIRC -> circle(t);
            case OUT_CIRC -> out(circle(1.0F - t));
            case IN_OUT_CIRC -> inOut(t, arg);
            case IN_BACK -> back(t, arg);
            case OUT_BACK -> out(back(1.0F - t, arg));
            case IN_OUT_BACK -> inOut(t, arg);
            case IN_ELASTIC -> elastic(t, arg);
            case OUT_ELASTIC -> out(elastic(1.0F - t, arg));
            case IN_OUT_ELASTIC -> inOut(t, arg);
            case IN_BOUNCE -> bounce(t, arg);
            case OUT_BOUNCE -> out(bounce(1.0F - t, arg));
            case IN_OUT_BOUNCE -> inOut(t, arg);
        };
    }

    private BedrockEasing inward() {
        return switch (this) {
            case IN_OUT_SINE -> IN_SINE;
            case IN_OUT_QUAD -> IN_QUAD;
            case IN_OUT_CUBIC -> IN_CUBIC;
            case IN_OUT_QUART -> IN_QUART;
            case IN_OUT_QUINT -> IN_QUINT;
            case IN_OUT_EXPO -> IN_EXPO;
            case IN_OUT_CIRC -> IN_CIRC;
            case IN_OUT_BACK -> IN_BACK;
            case IN_OUT_ELASTIC -> IN_ELASTIC;
            case IN_OUT_BOUNCE -> IN_BOUNCE;
            default -> LINEAR;
        };
    }

    private float inOut(float t, float arg) {
        BedrockEasing base = inward();
        if (t < 0.5F) return base.ease(t * 2.0F, arg) * 0.5F;
        return 1.0F - base.ease((1.0F - t) * 2.0F, arg) * 0.5F;
    }

    private static float out(float value) {
        return 1.0F - value;
    }

    private static float sine(float t) {
        return 1.0F - (float) Math.cos(t * Math.PI / 2.0);
    }

    private static float cube(float t) {
        return t * t * t;
    }

    private static float pow(float t, int exponent) {
        float result = t;
        for (int i = 1; i < exponent; i++) {
            result *= t;
        }
        return result;
    }

    private static float expo(float t) {
        return (float) Math.pow(2.0, 10.0 * (t - 1.0));
    }

    private static float circle(float t) {
        return 1.0F - (float) Math.sqrt(Math.max(0.0F, 1.0F - t * t));
    }

    private static float back(float t, float arg) {
        float scale = (Float.isNaN(arg) ? 1.0F : arg) * BACK_SCALE;
        return t * t * ((scale + 1.0F) * t - scale);
    }

    private static float elastic(float t, float arg) {
        float n = Float.isNaN(arg) ? 1.0F : arg;
        float damped = (float) Math.pow(Math.cos(t * Math.PI / 2.0), 3.0);
        return 1.0F - damped * (float) Math.cos(t * n * Math.PI);
    }

    private static float bounce(float t, float arg) {
        float n = Float.isNaN(arg) ? 0.5F : arg;
        float one = 121.0F / 16.0F * t * t;
        float two = 121.0F / 4.0F * n * sq(t - 6.0F / 11.0F) + 1.0F - n;
        float three = 121.0F * n * n * sq(t - 9.0F / 11.0F) + 1.0F - n * n;
        float four = 484.0F * n * n * n * sq(t - 10.5F / 11.0F) + 1.0F - n * n * n;
        return Math.min(Math.min(one, two), Math.min(three, four));
    }

    private static float sq(float value) {
        return value * value;
    }
}
