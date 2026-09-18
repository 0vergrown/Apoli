package dev.overgrown.apoli.data;

import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BodyParts {
    private BodyParts() {}

    private static final Map<String, BodyPart> BY_KEY = new HashMap<>();
    private static final Map<String, BodyPart> CANONICAL = new LinkedHashMap<>();
    private static final Map<String, BodyPart> CUSTOM = new ConcurrentHashMap<>();

    private static final float INF = Float.POSITIVE_INFINITY;

    public static final BodyPart HEAD = limb("head", BodyPart.HEAD | BodyPart.HAT, HumanoidPose.HEAD, ModelParts.HEAD);
    public static final BodyPart HAT = limb("hat", BodyPart.HAT, HumanoidPose.HEAD, ModelParts.HAT,
        "headwear", "hat_layer", "head_layer");
    public static final BodyPart BODY = limb("body", BodyPart.BODY | BodyPart.JACKET, HumanoidPose.BODY, ModelParts.BODY,
        "torso", "waist");
    public static final BodyPart JACKET = limb("jacket", BodyPart.JACKET, HumanoidPose.BODY, ModelParts.BODY,
        "body_layer");
    public static final BodyPart RIGHT_ARM = limb("right_arm", BodyPart.RIGHT_ARM | BodyPart.RIGHT_SLEEVE,
        HumanoidPose.RIGHT_ARM, ModelParts.RIGHT_ARM, "arm_right");
    public static final BodyPart RIGHT_SLEEVE = limb("right_sleeve", BodyPart.RIGHT_SLEEVE,
        HumanoidPose.RIGHT_ARM, ModelParts.RIGHT_ARM, "right_arm_layer");
    public static final BodyPart LEFT_ARM = limb("left_arm", BodyPart.LEFT_ARM | BodyPart.LEFT_SLEEVE,
        HumanoidPose.LEFT_ARM, ModelParts.LEFT_ARM, "arm_left");
    public static final BodyPart LEFT_SLEEVE = limb("left_sleeve", BodyPart.LEFT_SLEEVE,
        HumanoidPose.LEFT_ARM, ModelParts.LEFT_ARM, "left_arm_layer");
    public static final BodyPart RIGHT_LEG = limb("right_leg", BodyPart.RIGHT_LEG | BodyPart.RIGHT_PANTS,
        HumanoidPose.RIGHT_LEG, ModelParts.RIGHT_LEG, "leg_right");
    public static final BodyPart RIGHT_PANTS = limb("right_pants", BodyPart.RIGHT_PANTS,
        HumanoidPose.RIGHT_LEG, ModelParts.RIGHT_LEG, "right_leg_layer");
    public static final BodyPart LEFT_LEG = limb("left_leg", BodyPart.LEFT_LEG | BodyPart.LEFT_PANTS,
        HumanoidPose.LEFT_LEG, ModelParts.LEFT_LEG, "leg_left");
    public static final BodyPart LEFT_PANTS = limb("left_pants", BodyPart.LEFT_PANTS,
        HumanoidPose.LEFT_LEG, ModelParts.LEFT_LEG, "left_leg_layer");

    public static final BodyPart RIGHT_HAND = region("right_hand", BodyPart.LIMB_RIGHT_ARM,
        -INF, 7, -INF, INF, INF, INF,
        BodyPart.POINT_LIMB, HumanoidPose.RIGHT_ARM, 0, HumanoidPose.ARM_LENGTH, 0, -1, 0, 0, 0,
        "hand_right", "right_fist");
    public static final BodyPart LEFT_HAND = region("left_hand", BodyPart.LIMB_LEFT_ARM,
        -INF, 7, -INF, INF, INF, INF,
        BodyPart.POINT_LIMB, HumanoidPose.LEFT_ARM, 0, HumanoidPose.ARM_LENGTH, 0, -1, 0, 0, 0,
        "hand_left", "left_fist");
    public static final BodyPart MAIN_HAND = handed("main_hand", RIGHT_HAND, LEFT_HAND, "hand_main");
    public static final BodyPart OFF_HAND = handed("off_hand", LEFT_HAND, RIGHT_HAND, "hand_off");
    public static final BodyPart HANDS = region("hands", BodyPart.LIMB_RIGHT_ARM | BodyPart.LIMB_LEFT_ARM,
        -INF, 7, -INF, INF, INF, INF,
        BodyPart.POINT_MIDPOINT, HumanoidPose.RIGHT_ARM, 0, HumanoidPose.ARM_LENGTH, 0,
        HumanoidPose.LEFT_ARM, 0, HumanoidPose.ARM_LENGTH, 0);

    public static final BodyPart RIGHT_FOOT = region("right_foot", BodyPart.LIMB_RIGHT_LEG,
        -INF, 9, -INF, INF, INF, INF,
        BodyPart.POINT_LIMB, HumanoidPose.RIGHT_LEG, 0, HumanoidPose.LEG_LENGTH, 0, -1, 0, 0, 0,
        "foot_right");
    public static final BodyPart LEFT_FOOT = region("left_foot", BodyPart.LIMB_LEFT_LEG,
        -INF, 9, -INF, INF, INF, INF,
        BodyPart.POINT_LIMB, HumanoidPose.LEFT_LEG, 0, HumanoidPose.LEG_LENGTH, 0, -1, 0, 0, 0,
        "foot_left");
    public static final BodyPart FEET = region("feet", BodyPart.LIMB_RIGHT_LEG | BodyPart.LIMB_LEFT_LEG,
        -INF, 9, -INF, INF, INF, INF,
        BodyPart.POINT_MIDPOINT, HumanoidPose.RIGHT_LEG, 0, HumanoidPose.LEG_LENGTH, 0,
        HumanoidPose.LEFT_LEG, 0, HumanoidPose.LEG_LENGTH, 0);

    public static final BodyPart CHEST = region("chest", BodyPart.LIMB_BODY,
        -INF, -INF, -INF, INF, 6, 0,
        BodyPart.POINT_LIMB, HumanoidPose.BODY, 0, 3, -2, -1, 0, 0, 0);
    public static final BodyPart BACK = region("back", BodyPart.LIMB_BODY,
        -INF, -INF, 0, INF, INF, INF,
        BodyPart.POINT_LIMB, HumanoidPose.BODY, 0, 6, 2, -1, 0, 0, 0);
    public static final BodyPart ACHILLES_HEEL = region("achilles_heel", BodyPart.LIMB_RIGHT_LEG | BodyPart.LIMB_LEFT_LEG,
        -INF, 9, 0, INF, INF, INF,
        BodyPart.POINT_MIDPOINT, HumanoidPose.RIGHT_LEG, 0, HumanoidPose.LEG_LENGTH, 2,
        HumanoidPose.LEFT_LEG, 0, HumanoidPose.LEG_LENGTH, 2);

    public static final BodyPart ARMS = group("arms",
        BodyPart.RIGHT_ARM | BodyPart.RIGHT_SLEEVE | BodyPart.LEFT_ARM | BodyPart.LEFT_SLEEVE,
        BodyPart.LIMB_RIGHT_ARM | BodyPart.LIMB_LEFT_ARM, false,
        BodyPart.POINT_MIDPOINT, HumanoidPose.RIGHT_ARM, 0, 0, 0, HumanoidPose.LEFT_ARM, 0, 0, 0);
    public static final BodyPart LEGS = group("legs",
        BodyPart.RIGHT_LEG | BodyPart.RIGHT_PANTS | BodyPart.LEFT_LEG | BodyPart.LEFT_PANTS,
        BodyPart.LIMB_RIGHT_LEG | BodyPart.LIMB_LEFT_LEG, false,
        BodyPart.POINT_MIDPOINT, HumanoidPose.RIGHT_LEG, 0, 0, 0, HumanoidPose.LEFT_LEG, 0, 0, 0);
    public static final BodyPart UPPER = group("upper",
        BodyPart.HEAD | BodyPart.HAT | BodyPart.BODY | BodyPart.JACKET
            | BodyPart.RIGHT_ARM | BodyPart.RIGHT_SLEEVE | BodyPart.LEFT_ARM | BodyPart.LEFT_SLEEVE,
        BodyPart.LIMB_HEAD | BodyPart.LIMB_BODY | BodyPart.LIMB_RIGHT_ARM | BodyPart.LIMB_LEFT_ARM, false,
        BodyPart.POINT_LIMB, HumanoidPose.BODY, 0, 12, 0, -1, 0, 0, 0,
        "upper_body");
    public static final BodyPart LOWER = group("lower",
        BodyPart.BODY | BodyPart.JACKET
            | BodyPart.RIGHT_LEG | BodyPart.RIGHT_PANTS | BodyPart.LEFT_LEG | BodyPart.LEFT_PANTS,
        BodyPart.LIMB_BODY | BodyPart.LIMB_RIGHT_LEG | BodyPart.LIMB_LEFT_LEG, false,
        BodyPart.POINT_LIMB, HumanoidPose.BODY, 0, 0, 0, -1, 0, 0, 0,
        "lower_body");
    public static final BodyPart WHOLE = group("whole", BodyPart.ALL_MODELS, BodyPart.ALL_LIMBS, true,
        BodyPart.POINT_FIXED, -1, 0, 24, 0, -1, 0, 0, 0,
        "any", "all");

    private static BodyPart limb(String name, int models, int limb, String bindKey, String... aliases) {
        return register(new BodyPart(name, ModelParts.normalize(name), models, 1 << limb, false, false, false,
            new BodyPart.Region[]{BodyPart.Region.whole(1 << limb)},
            BodyPart.POINT_LIMB, limb, 0, 0, 0, -1, 0, 0, 0, bindKey), aliases);
    }

    private static BodyPart region(String name, int limbs, float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ, int pointKind,
                                   int limbA, float ax, float ay, float az, int limbB, float bx, float by, float bz,
                                   String... aliases) {
        return register(new BodyPart(name, ModelParts.normalize(name), 0, limbs, false, false, false,
            new BodyPart.Region[]{new BodyPart.Region(limbs, minX, minY, minZ, maxX, maxY, maxZ)},
            pointKind, limbA, ax, ay, az, limbB, bx, by, bz, null), aliases);
    }

    private static BodyPart group(String name, int models, int limbs, boolean everything, int pointKind,
                                  int limbA, float ax, float ay, float az, int limbB, float bx, float by, float bz,
                                  String... aliases) {
        return register(new BodyPart(name, ModelParts.normalize(name), models, limbs, true, everything, false,
            new BodyPart.Region[]{BodyPart.Region.whole(limbs)},
            pointKind, limbA, ax, ay, az, limbB, bx, by, bz, null), aliases);
    }

    private static BodyPart handed(String name, BodyPart rightHanded, BodyPart leftHanded, String... aliases) {
        BodyPart part = new BodyPart(name, ModelParts.normalize(name), 0, rightHanded.limbs() | leftHanded.limbs(),
            false, false, false, new BodyPart.Region[0], BodyPart.POINT_NONE, -1, 0, 0, 0, -1, 0, 0, 0, null);
        part.handed(rightHanded, leftHanded);
        return register(part, aliases);
    }

    private static BodyPart register(BodyPart part, String... aliases) {
        BY_KEY.put(part.key(), part);
        CANONICAL.put(part.name(), part);
        for (String alias : aliases) {
            BY_KEY.put(ModelParts.normalize(alias), part);
        }
        return part;
    }

    @Nullable
    public static BodyPart lookup(String name) {
        return BY_KEY.get(ModelParts.normalize(name));
    }

    public static BodyPart lookupOrCustom(String name) {
        String key = ModelParts.normalize(name);
        BodyPart known = BY_KEY.get(key);
        if (known != null) return known;
        return CUSTOM.computeIfAbsent(key, k -> new BodyPart(name, k, 0, 0, false, false, true,
            new BodyPart.Region[0], BodyPart.POINT_NONE, -1, 0, 0, 0, -1, 0, 0, 0, null));
    }

    public static DataResult<BodyPart> strict(String name) {
        BodyPart known = BY_KEY.get(ModelParts.normalize(name));
        if (known != null) return DataResult.success(known);
        return DataResult.error(() -> "Unknown body part '" + name + "'. Expected one of: "
            + String.join(", ", CANONICAL.keySet()));
    }

    public static List<BodyPart> all() {
        return Collections.unmodifiableList(new ArrayList<>(CANONICAL.values()));
    }
}
