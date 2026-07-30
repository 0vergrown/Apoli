package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum Easing {
    LINEAR,
    STEP,
    CATMULLROM,
    SMOOTHSTEP,
    SMOOTHERSTEP,
    EASE_IN_SINE,
    EASE_OUT_SINE,
    EASE_IN_OUT_SINE,
    EASE_IN_QUAD,
    EASE_OUT_QUAD,
    EASE_IN_OUT_QUAD,
    EASE_IN_CUBIC,
    EASE_OUT_CUBIC,
    EASE_IN_OUT_CUBIC,
    EASE_IN_QUART,
    EASE_OUT_QUART,
    EASE_IN_OUT_QUART,
    EASE_IN_QUINT,
    EASE_OUT_QUINT,
    EASE_IN_OUT_QUINT,
    EASE_IN_EXPO,
    EASE_OUT_EXPO,
    EASE_IN_OUT_EXPO,
    EASE_IN_CIRC,
    EASE_OUT_CIRC,
    EASE_IN_OUT_CIRC,
    EASE_IN_BACK,
    EASE_OUT_BACK,
    EASE_IN_OUT_BACK,
    EASE_IN_ELASTIC,
    EASE_OUT_ELASTIC,
    EASE_IN_OUT_ELASTIC,
    EASE_IN_BOUNCE,
    EASE_OUT_BOUNCE,
    EASE_IN_OUT_BOUNCE;

    private static final float BACK_C1 = 1.70158F;
    private static final float BACK_C2 = BACK_C1 * 1.525F;
    private static final float BACK_C3 = BACK_C1 + 1.0F;
    private static final float ELASTIC_C4 = (float) (2.0 * Math.PI / 3.0);
    private static final float ELASTIC_C5 = (float) (2.0 * Math.PI / 4.5);
    private static final float BOUNCE_N1 = 7.5625F;
    private static final float BOUNCE_D1 = 2.75F;

    private static final Map<String, Easing> BY_NAME = new HashMap<>();

    static {
        for (Easing easing : values()) {
            BY_NAME.put(ModelParts.normalize(easing.name()), easing);
        }
        BY_NAME.put("none", LINEAR);
        BY_NAME.put("hold", STEP);
        BY_NAME.put("constant", STEP);
        BY_NAME.put("smooth", CATMULLROM);
        BY_NAME.put("easein", EASE_IN_QUAD);
        BY_NAME.put("easeout", EASE_OUT_QUAD);
        BY_NAME.put("easeinout", EASE_IN_OUT_QUAD);
    }

    public static final Codec<Easing> CODEC = Codec.STRING.comapFlatMap(
        string -> {
            Easing easing = BY_NAME.get(ModelParts.normalize(string));
            return easing != null
                ? DataResult.success(easing)
                : DataResult.error(() -> "Unknown easing: '" + string + "'");
        },
        easing -> easing.name().toLowerCase(Locale.ROOT)
    );

    public float apply(float t) {
        if (t <= 0.0F) return 0.0F;
        if (t >= 1.0F) return 1.0F;
        return switch (this) {
            case LINEAR, CATMULLROM -> t;
            case STEP -> 0.0F;
            case SMOOTHSTEP -> t * t * (3.0F - 2.0F * t);
            case SMOOTHERSTEP -> t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
            case EASE_IN_SINE -> 1.0F - (float) Math.cos(t * Math.PI / 2.0);
            case EASE_OUT_SINE -> (float) Math.sin(t * Math.PI / 2.0);
            case EASE_IN_OUT_SINE -> -((float) Math.cos(Math.PI * t) - 1.0F) / 2.0F;
            case EASE_IN_QUAD -> t * t;
            case EASE_OUT_QUAD -> 1.0F - (1.0F - t) * (1.0F - t);
            case EASE_IN_OUT_QUAD -> t < 0.5F ? 2.0F * t * t : 1.0F - pow(-2.0F * t + 2.0F, 2) / 2.0F;
            case EASE_IN_CUBIC -> t * t * t;
            case EASE_OUT_CUBIC -> 1.0F - pow(1.0F - t, 3);
            case EASE_IN_OUT_CUBIC -> t < 0.5F ? 4.0F * t * t * t : 1.0F - pow(-2.0F * t + 2.0F, 3) / 2.0F;
            case EASE_IN_QUART -> pow(t, 4);
            case EASE_OUT_QUART -> 1.0F - pow(1.0F - t, 4);
            case EASE_IN_OUT_QUART -> t < 0.5F ? 8.0F * pow(t, 4) : 1.0F - pow(-2.0F * t + 2.0F, 4) / 2.0F;
            case EASE_IN_QUINT -> pow(t, 5);
            case EASE_OUT_QUINT -> 1.0F - pow(1.0F - t, 5);
            case EASE_IN_OUT_QUINT -> t < 0.5F ? 16.0F * pow(t, 5) : 1.0F - pow(-2.0F * t + 2.0F, 5) / 2.0F;
            case EASE_IN_EXPO -> (float) Math.pow(2.0, 10.0 * t - 10.0);
            case EASE_OUT_EXPO -> 1.0F - (float) Math.pow(2.0, -10.0 * t);
            case EASE_IN_OUT_EXPO -> t < 0.5F
                ? (float) Math.pow(2.0, 20.0 * t - 10.0) / 2.0F
                : (2.0F - (float) Math.pow(2.0, -20.0 * t + 10.0)) / 2.0F;
            case EASE_IN_CIRC -> 1.0F - (float) Math.sqrt(1.0F - t * t);
            case EASE_OUT_CIRC -> (float) Math.sqrt(1.0F - pow(t - 1.0F, 2));
            case EASE_IN_OUT_CIRC -> t < 0.5F
                ? (1.0F - (float) Math.sqrt(1.0F - pow(2.0F * t, 2))) / 2.0F
                : ((float) Math.sqrt(1.0F - pow(-2.0F * t + 2.0F, 2)) + 1.0F) / 2.0F;
            case EASE_IN_BACK -> BACK_C3 * t * t * t - BACK_C1 * t * t;
            case EASE_OUT_BACK -> 1.0F + BACK_C3 * pow(t - 1.0F, 3) + BACK_C1 * pow(t - 1.0F, 2);
            case EASE_IN_OUT_BACK -> t < 0.5F
                ? pow(2.0F * t, 2) * ((BACK_C2 + 1.0F) * 2.0F * t - BACK_C2) / 2.0F
                : (pow(2.0F * t - 2.0F, 2) * ((BACK_C2 + 1.0F) * (2.0F * t - 2.0F) + BACK_C2) + 2.0F) / 2.0F;
            case EASE_IN_ELASTIC -> -(float) (Math.pow(2.0, 10.0 * t - 10.0) * Math.sin((t * 10.0F - 10.75F) * ELASTIC_C4));
            case EASE_OUT_ELASTIC -> (float) (Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0F - 0.75F) * ELASTIC_C4)) + 1.0F;
            case EASE_IN_OUT_ELASTIC -> t < 0.5F
                ? -(float) (Math.pow(2.0, 20.0 * t - 10.0) * Math.sin((20.0F * t - 11.125F) * ELASTIC_C5)) / 2.0F
                : (float) (Math.pow(2.0, -20.0 * t + 10.0) * Math.sin((20.0F * t - 11.125F) * ELASTIC_C5)) / 2.0F + 1.0F;
            case EASE_IN_BOUNCE -> 1.0F - outBounce(1.0F - t);
            case EASE_OUT_BOUNCE -> outBounce(t);
            case EASE_IN_OUT_BOUNCE -> t < 0.5F
                ? (1.0F - outBounce(1.0F - 2.0F * t)) / 2.0F
                : (1.0F + outBounce(2.0F * t - 1.0F)) / 2.0F;
        };
    }

    private static float outBounce(float t) {
        if (t < 1.0F / BOUNCE_D1) {
            return BOUNCE_N1 * t * t;
        } else if (t < 2.0F / BOUNCE_D1) {
            float u = t - 1.5F / BOUNCE_D1;
            return BOUNCE_N1 * u * u + 0.75F;
        } else if (t < 2.5F / BOUNCE_D1) {
            float u = t - 2.25F / BOUNCE_D1;
            return BOUNCE_N1 * u * u + 0.9375F;
        } else {
            float u = t - 2.625F / BOUNCE_D1;
            return BOUNCE_N1 * u * u + 0.984375F;
        }
    }

    private static float pow(float base, int exp) {
        float result = base;
        for (int i = 1; i < exp; i++) result *= base;
        return result;
    }
}
