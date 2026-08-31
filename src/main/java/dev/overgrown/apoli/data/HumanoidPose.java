package dev.overgrown.apoli.data;

import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ModifyModelPartsPower;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class HumanoidPose {

    public static final int HEAD = 0;
    public static final int BODY = 1;
    public static final int RIGHT_ARM = 2;
    public static final int LEFT_ARM = 3;
    public static final int RIGHT_LEG = 4;
    public static final int LEFT_LEG = 5;
    public static final int PART_COUNT = 6;

    public static final float ARM_LENGTH = 10.0F;
    public static final float LEG_LENGTH = 12.0F;

    private static final int STRIDE = 9;
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    private static final int X_ROT = 3;
    private static final int Y_ROT = 4;
    private static final int Z_ROT = 5;
    private static final int X_SCALE = 6;
    private static final int Y_SCALE = 7;
    private static final int Z_SCALE = 8;

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private static final int POSE_EMPTY = 0;
    private static final int POSE_ITEM = 1;
    private static final int POSE_BLOCK = 2;
    private static final int POSE_BOW = 3;
    private static final int POSE_SPEAR = 4;

    private static final int STALE_TIMELINE_LIMIT = 128;

    private static final Map<LivingEntity, Map<ModelPartTransformation, Integer>> TIMELINES = new WeakHashMap<>();

    private final float[] parts = new float[PART_COUNT * STRIDE];

    private HumanoidPose() {
        rest();
    }

    public static HumanoidPose of(Entity entity) {
        HumanoidPose pose = new HumanoidPose();
        if (entity instanceof LivingEntity living) {
            pose.animate(living);
            pose.applyPowers(living);
        }
        return pose;
    }

    public float x(int part) {
        return parts[part * STRIDE + X];
    }

    public float y(int part) {
        return parts[part * STRIDE + Y];
    }

    public float z(int part) {
        return parts[part * STRIDE + Z];
    }

    public float xRot(int part) {
        return parts[part * STRIDE + X_ROT];
    }

    public float yRot(int part) {
        return parts[part * STRIDE + Y_ROT];
    }

    public float zRot(int part) {
        return parts[part * STRIDE + Z_ROT];
    }

    public float yScale(int part) {
        return parts[part * STRIDE + Y_SCALE];
    }

    private void rest() {
        for (int part = 0; part < PART_COUNT; part++) {
            int base = part * STRIDE;
            parts[base + X_SCALE] = 1.0F;
            parts[base + Y_SCALE] = 1.0F;
            parts[base + Z_SCALE] = 1.0F;
        }
        set(RIGHT_ARM, -5.0F, 2.0F, 0.0F);
        set(LEFT_ARM, 5.0F, 2.0F, 0.0F);
        set(RIGHT_LEG, -1.9F, 12.0F, 0.0F);
        set(LEFT_LEG, 1.9F, 12.0F, 0.0F);
    }

    private void set(int part, float x, float y, float z) {
        int base = part * STRIDE;
        parts[base + X] = x;
        parts[base + Y] = y;
        parts[base + Z] = z;
    }

    private float get(int part, int field) {
        return parts[part * STRIDE + field];
    }

    private void put(int part, int field, float value) {
        parts[part * STRIDE + field] = value;
    }

    private void add(int part, int field, float value) {
        parts[part * STRIDE + field] += value;
    }

    private void animate(LivingEntity entity) {
        boolean overridden = overridesPose(entity);
        boolean fallFlying = !overridden && entity.getFallFlyingTicks() > 4;
        boolean riding = !overridden && entity.isPassenger();
        boolean crouching = !overridden && entity.isCrouching();
        float swimAmount = overridden ? 0.0F : entity.getSwimAmount(1.0F);
        boolean visuallySwimming = entity.isVisuallySwimming();

        float netHeadYaw = Mth.wrapDegrees(entity.getYHeadRot() - entity.yBodyRot);
        float headPitch = entity.getXRot();
        float ageInTicks = entity.tickCount;
        float attackTime = entity.getAttackAnim(1.0F);

        float limbSwing = 0.0F;
        float limbSwingAmount = 0.0F;
        if (!entity.isPassenger() && entity.isAlive()) {
            limbSwingAmount = Math.min(1.0F, walkSpeed(entity));
            limbSwing = walkPosition(entity);
            if (entity.isBaby()) limbSwing *= 3.0F;
        }

        put(HEAD, Y_ROT, netHeadYaw * DEG_TO_RAD);
        if (fallFlying) {
            put(HEAD, X_ROT, (float) (-Math.PI / 4));
        } else if (swimAmount > 0.0F) {
            put(HEAD, X_ROT, visuallySwimming
                ? rotlerpRad(swimAmount, get(HEAD, X_ROT), (float) (-Math.PI / 4))
                : rotlerpRad(swimAmount, get(HEAD, X_ROT), headPitch * DEG_TO_RAD));
        } else {
            put(HEAD, X_ROT, headPitch * DEG_TO_RAD);
        }

        put(BODY, Y_ROT, 0.0F);
        put(RIGHT_ARM, Z, 0.0F);
        put(RIGHT_ARM, X, -5.0F);
        put(LEFT_ARM, Z, 0.0F);
        put(LEFT_ARM, X, 5.0F);

        float damping = 1.0F;
        if (fallFlying) {
            damping = (float) entity.getDeltaMovement().lengthSqr();
            damping /= 0.2F;
            damping *= damping * damping;
        }
        if (damping < 1.0F) damping = 1.0F;

        put(RIGHT_ARM, X_ROT, Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F / damping);
        put(LEFT_ARM, X_ROT, Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F / damping);
        put(RIGHT_ARM, Z_ROT, 0.0F);
        put(LEFT_ARM, Z_ROT, 0.0F);
        put(RIGHT_LEG, X_ROT, Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount / damping);
        put(LEFT_LEG, X_ROT, Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount / damping);
        put(RIGHT_LEG, Y_ROT, 0.005F);
        put(LEFT_LEG, Y_ROT, -0.005F);
        put(RIGHT_LEG, Z_ROT, 0.005F);
        put(LEFT_LEG, Z_ROT, -0.005F);

        if (riding) {
            add(RIGHT_ARM, X_ROT, (float) (-Math.PI / 5));
            add(LEFT_ARM, X_ROT, (float) (-Math.PI / 5));
            put(RIGHT_LEG, X_ROT, -1.4137167F);
            put(RIGHT_LEG, Y_ROT, (float) (Math.PI / 10));
            put(RIGHT_LEG, Z_ROT, 0.07853982F);
            put(LEFT_LEG, X_ROT, -1.4137167F);
            put(LEFT_LEG, Y_ROT, (float) (-Math.PI / 10));
            put(LEFT_LEG, Z_ROT, -0.07853982F);
        }

        put(RIGHT_ARM, Y_ROT, 0.0F);
        put(LEFT_ARM, Y_ROT, 0.0F);

        HumanoidArm mainArm = entity.getMainArm();
        boolean mainIsRight = mainArm == HumanoidArm.RIGHT;
        int rightPose = armPose(entity, mainIsRight ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        int leftPose = armPose(entity, mainIsRight ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (entity.isUsingItem()) {
            if ((entity.getUsedItemHand() == InteractionHand.MAIN_HAND) == mainIsRight) {
                poseArm(RIGHT_ARM, rightPose, true);
            } else {
                poseArm(LEFT_ARM, leftPose, false);
            }
        } else {
            poseArm(RIGHT_ARM, rightPose, true);
            poseArm(LEFT_ARM, leftPose, false);
        }

        attackAnimation(entity, attackTime, mainArm);

        if (crouching) {
            put(BODY, X_ROT, 0.5F);
            add(RIGHT_ARM, X_ROT, 0.4F);
            add(LEFT_ARM, X_ROT, 0.4F);
            put(RIGHT_LEG, Z, 4.0F);
            put(LEFT_LEG, Z, 4.0F);
            put(RIGHT_LEG, Y, 12.2F);
            put(LEFT_LEG, Y, 12.2F);
            put(HEAD, Y, 4.2F);
            put(BODY, Y, 3.2F);
            put(LEFT_ARM, Y, 5.2F);
            put(RIGHT_ARM, Y, 5.2F);
        } else {
            put(BODY, X_ROT, 0.0F);
            put(RIGHT_LEG, Z, 0.0F);
            put(LEFT_LEG, Z, 0.0F);
            put(RIGHT_LEG, Y, 12.0F);
            put(LEFT_LEG, Y, 12.0F);
            put(HEAD, Y, 0.0F);
            put(BODY, Y, 0.0F);
            put(LEFT_ARM, Y, 2.0F);
            put(RIGHT_ARM, Y, 2.0F);
        }

        bob(RIGHT_ARM, ageInTicks, 1.0F);
        bob(LEFT_ARM, ageInTicks, -1.0F);

        if (swimAmount > 0.0F) {
            swim(entity, limbSwing, swimAmount, attackTime, mainArm);
        }
    }

    private void poseArm(int arm, int pose, boolean right) {
        switch (pose) {
            case POSE_ITEM -> {
                put(arm, X_ROT, get(arm, X_ROT) * 0.5F - (float) (Math.PI / 10));
                put(arm, Y_ROT, 0.0F);
            }
            case POSE_BLOCK -> {
                put(arm, X_ROT, get(arm, X_ROT) * 0.5F - 0.9424779F
                    + Mth.clamp(get(HEAD, X_ROT), (float) (-Math.PI * 4.0 / 9.0), 0.43633232F));
                put(arm, Y_ROT, (right ? -30.0F : 30.0F) * DEG_TO_RAD
                    + Mth.clamp(get(HEAD, Y_ROT), (float) (-Math.PI / 6), (float) (Math.PI / 6)));
            }
            case POSE_BOW -> {
                put(RIGHT_ARM, Y_ROT, -0.1F + get(HEAD, Y_ROT) + (right ? 0.0F : -0.4F));
                put(LEFT_ARM, Y_ROT, 0.1F + get(HEAD, Y_ROT) + (right ? 0.4F : 0.0F));
                put(RIGHT_ARM, X_ROT, (float) (-Math.PI / 2) + get(HEAD, X_ROT));
                put(LEFT_ARM, X_ROT, (float) (-Math.PI / 2) + get(HEAD, X_ROT));
            }
            case POSE_SPEAR -> {
                put(arm, X_ROT, get(arm, X_ROT) * 0.5F - (float) Math.PI);
                put(arm, Y_ROT, 0.0F);
            }
            default -> put(arm, Y_ROT, 0.0F);
        }
    }

    private void attackAnimation(LivingEntity entity, float attackTime, HumanoidArm mainArm) {
        if (attackTime <= 0.0F) return;
        HumanoidArm attacking = entity.swingingArm == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
        int arm = attacking == HumanoidArm.RIGHT ? RIGHT_ARM : LEFT_ARM;

        float bodyYRot = Mth.sin(Mth.sqrt(attackTime) * (float) (Math.PI * 2)) * 0.2F;
        if (attacking == HumanoidArm.LEFT) bodyYRot *= -1.0F;
        put(BODY, Y_ROT, bodyYRot);

        put(RIGHT_ARM, Z, Mth.sin(bodyYRot) * 5.0F);
        put(RIGHT_ARM, X, -Mth.cos(bodyYRot) * 5.0F);
        put(LEFT_ARM, Z, -Mth.sin(bodyYRot) * 5.0F);
        put(LEFT_ARM, X, Mth.cos(bodyYRot) * 5.0F);
        add(RIGHT_ARM, Y_ROT, bodyYRot);
        add(LEFT_ARM, Y_ROT, bodyYRot);
        add(LEFT_ARM, X_ROT, bodyYRot);

        float eased = 1.0F - attackTime;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;
        float swing = Mth.sin(eased * (float) Math.PI);
        float lean = Mth.sin(attackTime * (float) Math.PI) * -(get(HEAD, X_ROT) - 0.7F) * 0.75F;
        add(arm, X_ROT, -(swing * 1.2F + lean));
        add(arm, Y_ROT, bodyYRot * 2.0F);
        add(arm, Z_ROT, Mth.sin(attackTime * (float) Math.PI) * -0.4F);
    }

    private void swim(LivingEntity entity, float limbSwing, float swimAmount, float attackTime, HumanoidArm mainArm) {
        float cycle = limbSwing % 26.0F;
        HumanoidArm attacking = entity.swingingArm == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
        float right = attacking == HumanoidArm.RIGHT && attackTime > 0.0F ? 0.0F : swimAmount;
        float left = attacking == HumanoidArm.LEFT && attackTime > 0.0F ? 0.0F : swimAmount;
        if (!entity.isUsingItem()) {
            if (cycle < 14.0F) {
                put(LEFT_ARM, X_ROT, rotlerpRad(left, get(LEFT_ARM, X_ROT), 0.0F));
                put(RIGHT_ARM, X_ROT, Mth.lerp(right, get(RIGHT_ARM, X_ROT), 0.0F));
                put(LEFT_ARM, Y_ROT, rotlerpRad(left, get(LEFT_ARM, Y_ROT), (float) Math.PI));
                put(RIGHT_ARM, Y_ROT, Mth.lerp(right, get(RIGHT_ARM, Y_ROT), (float) Math.PI));
                put(LEFT_ARM, Z_ROT, rotlerpRad(left, get(LEFT_ARM, Z_ROT),
                    (float) Math.PI + 1.8707964F * quadraticArmUpdate(cycle) / quadraticArmUpdate(14.0F)));
                put(RIGHT_ARM, Z_ROT, Mth.lerp(right, get(RIGHT_ARM, Z_ROT),
                    (float) Math.PI - 1.8707964F * quadraticArmUpdate(cycle) / quadraticArmUpdate(14.0F)));
            } else if (cycle < 22.0F) {
                float t = (cycle - 14.0F) / 8.0F;
                put(LEFT_ARM, X_ROT, rotlerpRad(left, get(LEFT_ARM, X_ROT), (float) (Math.PI / 2) * t));
                put(RIGHT_ARM, X_ROT, Mth.lerp(right, get(RIGHT_ARM, X_ROT), (float) (Math.PI / 2) * t));
                put(LEFT_ARM, Y_ROT, rotlerpRad(left, get(LEFT_ARM, Y_ROT), (float) Math.PI));
                put(RIGHT_ARM, Y_ROT, Mth.lerp(right, get(RIGHT_ARM, Y_ROT), (float) Math.PI));
                put(LEFT_ARM, Z_ROT, rotlerpRad(left, get(LEFT_ARM, Z_ROT), 5.012389F - 1.8707964F * t));
                put(RIGHT_ARM, Z_ROT, Mth.lerp(right, get(RIGHT_ARM, Z_ROT), 1.2707963F + 1.8707964F * t));
            } else if (cycle < 26.0F) {
                float t = (cycle - 22.0F) / 4.0F;
                put(LEFT_ARM, X_ROT, rotlerpRad(left, get(LEFT_ARM, X_ROT), (float) (Math.PI / 2) - (float) (Math.PI / 2) * t));
                put(RIGHT_ARM, X_ROT, Mth.lerp(right, get(RIGHT_ARM, X_ROT), (float) (Math.PI / 2) - (float) (Math.PI / 2) * t));
                put(LEFT_ARM, Y_ROT, rotlerpRad(left, get(LEFT_ARM, Y_ROT), (float) Math.PI));
                put(RIGHT_ARM, Y_ROT, Mth.lerp(right, get(RIGHT_ARM, Y_ROT), (float) Math.PI));
                put(LEFT_ARM, Z_ROT, rotlerpRad(left, get(LEFT_ARM, Z_ROT), (float) Math.PI));
                put(RIGHT_ARM, Z_ROT, Mth.lerp(right, get(RIGHT_ARM, Z_ROT), (float) Math.PI));
            }
        }
        put(LEFT_LEG, X_ROT, Mth.lerp(swimAmount, get(LEFT_LEG, X_ROT),
            0.3F * Mth.cos(limbSwing * 0.33333334F + (float) Math.PI)));
        put(RIGHT_LEG, X_ROT, Mth.lerp(swimAmount, get(RIGHT_LEG, X_ROT), 0.3F * Mth.cos(limbSwing * 0.33333334F)));
    }

    private void bob(int arm, float ageInTicks, float sign) {
        add(arm, Z_ROT, sign * (Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F));
        add(arm, X_ROT, sign * Mth.sin(ageInTicks * 0.067F) * 0.05F);
    }

    private void applyPowers(LivingEntity entity) {
        if (!ModifyModelPartsPower.has(entity)) {
            if (!TIMELINES.isEmpty()) TIMELINES.remove(entity);
            return;
        }
        Map<ModelPartTransformation, Integer> starts =
            TIMELINES.computeIfAbsent(entity, key -> new IdentityHashMap<>(4));
        int now = entity.tickCount;
        PowerLookup.forEach(entity, ModifyModelPartsPower.CANONICAL, ModifyModelPartsPower.Config.class, config -> {
            List<ModelPartTransformation> transformations = config.transformations();
            for (int i = 0; i < transformations.size(); i++) {
                ModelPartTransformation transformation = transformations.get(i);
                int mask = transformation.perspectiveMask();
                if (mask == Perspective.MASK_INHERIT) mask = config.perspectiveMask();
                if (!Perspective.masked(mask, false)) continue;
                int part = indexOf(transformation.normalizedPart());
                if (part < 0) continue;
                int start = starts.computeIfAbsent(transformation, key -> now);
                float value = transformation.sample(now - start, entity);
                float weight = weight(transformation, now - start);
                if (weight <= 0.0F) continue;
                apply(part, transformation, value, weight);
            }
        });
        if (starts.size() > STALE_TIMELINE_LIMIT) starts.clear();
    }

    private void apply(int part, ModelPartTransformation transformation, float value, float weight) {
        boolean override = transformation.overrideAnimation();
        switch (transformation.type()) {
            case PITCH -> put(part, X_ROT, override
                ? get(part, X_ROT) + (value - get(part, X_ROT)) * weight
                : get(part, X_ROT) + value * weight);
            case YAW -> put(part, Y_ROT, override
                ? get(part, Y_ROT) + (value - get(part, Y_ROT)) * weight
                : get(part, Y_ROT) + value * weight);
            case ROLL -> put(part, Z_ROT, override
                ? get(part, Z_ROT) + (value - get(part, Z_ROT)) * weight
                : get(part, Z_ROT) + value * weight);
            case X_SCALE -> put(part, X_SCALE, 1.0F + value * weight);
            case Y_SCALE -> put(part, Y_SCALE, 1.0F + value * weight);
            case Z_SCALE -> put(part, Z_SCALE, 1.0F + value * weight);
            case PIVOT_X -> add(part, X, value * weight);
            case PIVOT_Y -> add(part, Y, value * weight);
            case PIVOT_Z -> add(part, Z, value * weight);
            default -> {}
        }
    }

    private static float weight(ModelPartTransformation transformation, float elapsed) {
        float duration = transformation.duration();
        if (duration <= 0.0F || elapsed >= duration) return 1.0F;
        return transformation.easing().apply(elapsed / duration);
    }

    public static boolean overridesPose(LivingEntity entity) {
        if (!ModifyModelPartsPower.has(entity)) return false;
        boolean[] overridden = {false};
        PowerLookup.forEach(entity, ModifyModelPartsPower.CANONICAL, ModifyModelPartsPower.Config.class, config -> {
            if (ModifyModelPartsPower.masked(config.overridePoseMask(), entity.getPose())) overridden[0] = true;
        });
        return overridden[0];
    }

    public static int indexOf(String normalizedPart) {
        String slot = ModelParts.slot(normalizedPart);
        if (slot == null) return -1;
        return switch (slot) {
            case ModelParts.HEAD, ModelParts.HAT -> HEAD;
            case ModelParts.BODY -> BODY;
            case ModelParts.RIGHT_ARM -> RIGHT_ARM;
            case ModelParts.LEFT_ARM -> LEFT_ARM;
            case ModelParts.RIGHT_LEG -> RIGHT_LEG;
            case ModelParts.LEFT_LEG -> LEFT_LEG;
            default -> -1;
        };
    }

    private static int armPose(LivingEntity entity, InteractionHand hand) {
        ItemStack stack = entity.getItemInHand(hand);
        if (stack.isEmpty()) return POSE_EMPTY;
        if (entity.isUsingItem() && entity.getUsedItemHand() == hand && entity.getUseItemRemainingTicks() > 0) {
            UseAnim anim = stack.getUseAnimation();
            if (anim == UseAnim.BLOCK) return POSE_BLOCK;
            if (anim == UseAnim.BOW) return POSE_BOW;
            if (anim == UseAnim.SPEAR) return POSE_SPEAR;
        }
        return POSE_ITEM;
    }

    private static float walkSpeed(LivingEntity entity) {
        return entity.walkAnimation.speed(1.0F);
    }

    private static float walkPosition(LivingEntity entity) {
        return entity.walkAnimation.position(1.0F);
    }

    private static float rotlerpRad(float delta, float from, float to) {
        float diff = (to - from) % (float) (Math.PI * 2);
        if (diff < (float) -Math.PI) diff += (float) (Math.PI * 2);
        if (diff >= (float) Math.PI) diff -= (float) (Math.PI * 2);
        return from + delta * diff;
    }

    private static float quadraticArmUpdate(float value) {
        return -65.0F * value + value * value;
    }
}
