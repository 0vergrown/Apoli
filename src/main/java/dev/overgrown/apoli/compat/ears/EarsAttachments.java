package dev.overgrown.apoli.compat.ears;

import com.unascribed.ears.api.EarsFeatureType;
import com.unascribed.ears.api.features.EarsFeatures;
import com.unascribed.ears.common.render.AbstractEarsRenderDelegate;
import dev.overgrown.apoli.data.BodyAttachments;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

@Environment(EnvType.CLIENT)
public final class EarsAttachments {
    public static final boolean SKEWED_QUADS = declares("renderFrontSkew");

    private static final int[] FEATURE_BITS = buildFeatureBits();

    private EarsAttachments() {}

    public static int featureBits(EarsFeatureType feature) {
        return FEATURE_BITS[feature.ordinal()];
    }

    public static int bit(@Nullable EarsFeatureType feature, @Nullable EarsFeatures features, int quad) {
        if (feature == null) return 0;
        int bits = FEATURE_BITS[feature.ordinal()];
        if (bits == BodyAttachments.EARS) return features == null ? BodyAttachments.EAR_PAIR : ear(features.earMode, quad);
        if (bits == BodyAttachments.WINGS) return features == null ? BodyAttachments.WING_PAIR : wing(features.wingMode, quad);
        return bits;
    }

    public static int tagBits(String tag) {
        return switch (tag) {
            case "ears" -> BodyAttachments.EAR_PAIR;
            case "wing", "animated-wing" -> BodyAttachments.WING_PAIR;
            case "horn" -> BodyAttachments.HORNS;
            case "snout" -> BodyAttachments.SNOUT;
            case "tail" -> BodyAttachments.TAIL;
            case "claw_right_arm" -> BodyAttachments.RIGHT_ARM_CLAW;
            case "claw_left_arm" -> BodyAttachments.LEFT_ARM_CLAW;
            case "claw_right_leg" -> BodyAttachments.RIGHT_LEG_CLAW;
            case "claw_left_leg" -> BodyAttachments.LEFT_LEG_CLAW;
            case "chest" -> BodyAttachments.CHEST;
            case "cape" -> BodyAttachments.CAPE;
            case "halo", "double_halo" -> BodyAttachments.HALO;
            case "digitigrade_pant" -> BodyAttachments.DIGITIGRADE_LEGS;
            default -> 0;
        };
    }

    public static int subtagBits(int group, int current, String subtag) {
        if (group == BodyAttachments.EAR_PAIR) {
            return switch (subtag) {
                case "right" -> BodyAttachments.RIGHT_EAR;
                case "left" -> BodyAttachments.LEFT_EAR;
                default -> BodyAttachments.EAR_PAIR;
            };
        }
        if (group == BodyAttachments.WING_PAIR) {
            return switch (subtag) {
                case "sym_right", "asym_left" -> BodyAttachments.RIGHT_WING;
                case "sym_left", "asym_right" -> BodyAttachments.LEFT_WING;
                default -> BodyAttachments.WING_PAIR;
            };
        }
        if (group == BodyAttachments.DIGITIGRADE_LEGS) {
            if (subtag.startsWith("right_")) return BodyAttachments.RIGHT_DIGITIGRADE_LEG;
            if (subtag.startsWith("left_")) return BodyAttachments.LEFT_DIGITIGRADE_LEG;
        }
        return current;
    }

    private static int[] buildFeatureBits() {
        EarsFeatureType[] types = EarsFeatureType.values();
        int[] bits = new int[types.length];
        for (int i = 0; i < types.length; i++) {
            bits[i] = featureBits(types[i].name());
        }
        return bits;
    }

    private static int featureBits(String name) {
        return switch (name) {
            case "EARS" -> BodyAttachments.EARS;
            case "WINGS" -> BodyAttachments.WINGS;
            case "HORN" -> BodyAttachments.HORNS;
            case "SNOUT" -> BodyAttachments.SNOUT;
            case "TAIL" -> BodyAttachments.TAIL;
            case "CLAW_RIGHT_ARM" -> BodyAttachments.RIGHT_ARM_CLAW;
            case "CLAW_LEFT_ARM" -> BodyAttachments.LEFT_ARM_CLAW;
            case "CLAW_RIGHT_LEG" -> BodyAttachments.RIGHT_LEG_CLAW;
            case "CLAW_LEFT_LEG" -> BodyAttachments.LEFT_LEG_CLAW;
            case "CHEST" -> BodyAttachments.CHEST;
            case "CAPE" -> BodyAttachments.CAPE;
            case "HALO" -> BodyAttachments.HALO;
            case "DIGITIGRADE_RIGHT_LEG" -> BodyAttachments.RIGHT_DIGITIGRADE_LEG;
            case "DIGITIGRADE_LEFT_LEG" -> BodyAttachments.LEFT_DIGITIGRADE_LEG;
            default -> 0;
        };
    }

    private static int ear(@Nullable EarsFeatures.EarMode mode, int quad) {
        if (mode == null) return BodyAttachments.EAR_PAIR;
        return switch (mode) {
            case SIDES, BEHIND, FLOPPY, OUT -> quad < 2 ? BodyAttachments.RIGHT_EAR : BodyAttachments.LEFT_EAR;
            case AROUND -> quad < 2 ? BodyAttachments.EAR_PAIR
                : quad < 4 ? BodyAttachments.RIGHT_EAR : BodyAttachments.LEFT_EAR;
            default -> BodyAttachments.EAR_PAIR;
        };
    }

    private static int wing(@Nullable EarsFeatures.WingMode mode, int quad) {
        if (mode == null) return BodyAttachments.WING_PAIR;
        return switch (mode) {
            case SYMMETRIC_DUAL, ASYMMETRIC_DUAL -> quad < 2 ? BodyAttachments.RIGHT_WING : BodyAttachments.LEFT_WING;
            case ASYMMETRIC_R -> BodyAttachments.RIGHT_WING;
            case ASYMMETRIC_L -> BodyAttachments.LEFT_WING;
            default -> BodyAttachments.WING_PAIR;
        };
    }

    private static boolean declares(String name) {
        for (Method method : AbstractEarsRenderDelegate.class.getDeclaredMethods()) {
            if (method.getName().equals(name)) return true;
        }
        return false;
    }
}
