package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.overgrown.apoli.access.ModifiedPoseHolder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class EntityPoses {
    private EntityPoses() {}

    public static final Codec<Pose> CODEC = Codec.STRING.comapFlatMap(
        EntityPoses::byName,
        pose -> pose.name().toLowerCase(Locale.ROOT));

    public static final int MAX_TICKS = 1200;
    public static final float PLAYER_MODEL_SCALE = 0.9375F;

    private static final float BLEND_TICKS = 4.0F;
    private static final float HALF_PI = (float) (Math.PI / 2);

    private static final int STRIDE = PoseMath.STRIDE;
    private static final int X_ROT = PoseMath.X_ROT;
    private static final int Y_ROT = PoseMath.Y_ROT;
    private static final int Z_ROT = PoseMath.Z_ROT;
    private static final int Y = PoseMath.Y;
    private static final int Z = PoseMath.Z;
    private static final int HEAD = HumanoidPose.HEAD;
    private static final int BODY = HumanoidPose.BODY;
    private static final int RIGHT_ARM = HumanoidPose.RIGHT_ARM;
    private static final int LEFT_ARM = HumanoidPose.LEFT_ARM;
    private static final int RIGHT_LEG = HumanoidPose.RIGHT_LEG;
    private static final int LEFT_LEG = HumanoidPose.LEFT_LEG;

    private static DataResult<Pose> byName(String name) {
        try {
            return DataResult.success(Pose.valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Unknown entity pose: '" + name + "'");
        }
    }

    @Nullable
    public static Pose forced(Entity entity) {
        return entity instanceof ModifiedPoseHolder holder ? holder.apoli$getModifiedEntityPose() : null;
    }

    public static int forcedTicks(Entity entity) {
        return entity instanceof ModifiedPoseHolder holder ? holder.apoli$getPoseTicks() : 0;
    }

    public static float progress(Entity entity, float partialTick) {
        return Math.min(1.0F, (forcedTicks(entity) + partialTick) / BLEND_TICKS);
    }

    public static int fallFlyingTicks(Entity entity, int original) {
        if (original > 0 || forced(entity) != Pose.FALL_FLYING) return original;
        return forcedTicks(entity);
    }

    public static int deathTime(Entity entity, int original) {
        if (original > 0 || forced(entity) != Pose.DYING) return original;
        return forcedTicks(entity) + 1;
    }

    @Nullable
    public static EntityDimensions playerDimensions(Pose pose) {
        return switch (pose) {
            case SITTING -> PlayerDimensions.SITTING;
            case SLIDING -> PlayerDimensions.SLIDING;
            default -> null;
        };
    }

    public static boolean resizesPlayer(@Nullable Pose pose) {
        return pose == Pose.SITTING || pose == Pose.SLIDING;
    }

    public static boolean holdsStill(Pose pose) {
        return pose == Pose.SITTING || pose == Pose.SLIDING || pose == Pose.DYING;
    }

    public static boolean movesBody(Pose pose) {
        return pose == Pose.SITTING || pose == Pose.SLIDING;
    }

    public static float bodyPitch(Pose pose) {
        return pose == Pose.SLIDING ? 72.0F : 0.0F;
    }

    public static float bodyDrop(Pose pose) {
        return switch (pose) {
            case SITTING -> 10.0F;
            case SLIDING -> 3.3F;
            default -> 0.0F;
        };
    }

    public static boolean shapes(Pose pose) {
        return switch (pose) {
            case SITTING, SLIDING, SHOOTING, INHALING, LONG_JUMPING, CROAKING, USING_TONGUE, ROARING, SNIFFING,
                 EMERGING, DIGGING -> true;
            default -> false;
        };
    }

    public static void shape(Pose pose, float[] parts, float t, float age) {
        switch (pose) {
            case SITTING -> sitting(parts, t);
            case SLIDING -> sliding(parts, t);
            case SHOOTING -> shooting(parts, t);
            case INHALING -> inhaling(parts, t, age);
            case LONG_JUMPING -> longJumping(parts, t);
            case CROAKING -> croaking(parts, t, age);
            case USING_TONGUE -> usingTongue(parts, t);
            case ROARING -> roaring(parts, t, age);
            case SNIFFING -> sniffing(parts, t, age);
            case EMERGING -> emerging(parts, t, age);
            case DIGGING -> digging(parts, t, age);
            default -> {}
        }
    }

    private static void sitting(float[] p, float t) {
        put(p, RIGHT_LEG, X_ROT, -HALF_PI, t);
        put(p, RIGHT_LEG, Y_ROT, 0.25F, t);
        put(p, RIGHT_LEG, Z_ROT, 0.0785F, t);
        put(p, LEFT_LEG, X_ROT, -HALF_PI, t);
        put(p, LEFT_LEG, Y_ROT, -0.25F, t);
        put(p, LEFT_LEG, Z_ROT, -0.0785F, t);
        add(p, RIGHT_ARM, X_ROT, -0.55F, t);
        add(p, LEFT_ARM, X_ROT, -0.55F, t);
    }

    private static void sliding(float[] p, float t) {
        float legs = -(HALF_PI - 72.0F * Mth.DEG_TO_RAD);
        put(p, RIGHT_LEG, X_ROT, legs, t);
        put(p, RIGHT_LEG, Y_ROT, 0.1F, t);
        put(p, LEFT_LEG, X_ROT, legs - 0.35F, t);
        put(p, LEFT_LEG, Y_ROT, -0.25F, t);
        add(p, RIGHT_ARM, Z_ROT, 0.7F, t);
        add(p, LEFT_ARM, X_ROT, -0.6F, t);
        add(p, LEFT_ARM, Z_ROT, -0.15F, t);
        add(p, HEAD, X_ROT, 0.85F, t);
    }

    private static void shooting(float[] p, float t) {
        float aim = get(p, HEAD, X_ROT) - HALF_PI;
        float yaw = get(p, HEAD, Y_ROT);
        put(p, RIGHT_ARM, X_ROT, aim, t);
        put(p, RIGHT_ARM, Y_ROT, yaw - 0.12F, t);
        put(p, RIGHT_ARM, Z_ROT, 0.0F, t);
        put(p, LEFT_ARM, X_ROT, aim, t);
        put(p, LEFT_ARM, Y_ROT, yaw + 0.12F, t);
        put(p, LEFT_ARM, Z_ROT, 0.0F, t);
    }

    private static void inhaling(float[] p, float t, float age) {
        float breath = Mth.sin(age * 0.1F) * 0.06F;
        lean(p, -0.12F, t, 0.0F);
        add(p, HEAD, X_ROT, -0.45F - breath, t);
        add(p, RIGHT_ARM, X_ROT, 0.35F, t);
        add(p, RIGHT_ARM, Z_ROT, 0.3F + breath, t);
        add(p, LEFT_ARM, X_ROT, 0.35F, t);
        add(p, LEFT_ARM, Z_ROT, -0.3F - breath, t);
    }

    private static void longJumping(float[] p, float t) {
        lean(p, 0.25F, t, 0.0F);
        put(p, RIGHT_LEG, X_ROT, -0.9F, t);
        put(p, LEFT_LEG, X_ROT, 0.7F, t);
        add(p, RIGHT_ARM, X_ROT, -1.5F, t);
        add(p, RIGHT_ARM, Y_ROT, 0.3F, t);
        add(p, LEFT_ARM, X_ROT, -1.5F, t);
        add(p, LEFT_ARM, Y_ROT, -0.3F, t);
    }

    private static void croaking(float[] p, float t, float age) {
        float puff = Math.abs(Mth.sin(age * 0.35F)) * 0.15F;
        add(p, HEAD, X_ROT, -0.3F, t);
        add(p, RIGHT_ARM, Z_ROT, 0.45F + puff, t);
        add(p, RIGHT_ARM, X_ROT, -0.1F, t);
        add(p, LEFT_ARM, Z_ROT, -0.45F - puff, t);
        add(p, LEFT_ARM, X_ROT, -0.1F, t);
        put(p, RIGHT_LEG, Z_ROT, 0.2F, t);
        put(p, LEFT_LEG, Z_ROT, -0.2F, t);
    }

    private static void usingTongue(float[] p, float t) {
        lean(p, 0.4F, t, 0.0F);
        add(p, HEAD, X_ROT, -0.55F, t);
        add(p, RIGHT_ARM, X_ROT, 0.7F, t);
        add(p, RIGHT_ARM, Z_ROT, 0.2F, t);
        add(p, LEFT_ARM, X_ROT, 0.7F, t);
        add(p, LEFT_ARM, Z_ROT, -0.2F, t);
    }

    private static void roaring(float[] p, float t, float age) {
        float shake = Mth.sin(age * 2.0F) * 0.04F;
        lean(p, -0.2F, t, 0.0F);
        add(p, HEAD, X_ROT, -0.6F + shake, t);
        add(p, RIGHT_ARM, Z_ROT, 1.2F + shake, t);
        add(p, RIGHT_ARM, X_ROT, 0.15F, t);
        add(p, LEFT_ARM, Z_ROT, -1.2F - shake, t);
        add(p, LEFT_ARM, X_ROT, 0.15F, t);
        put(p, RIGHT_LEG, Z_ROT, 0.12F, t);
        put(p, LEFT_LEG, Z_ROT, -0.12F, t);
    }

    private static void sniffing(float[] p, float t, float age) {
        lean(p, 0.45F, t, 0.8F);
        add(p, HEAD, X_ROT, 0.25F, t);
        add(p, HEAD, Y_ROT, Mth.sin(age * 0.14F) * 0.45F, t);
    }

    private static void emerging(float[] p, float t, float age) {
        float reach = Mth.sin(age * 0.25F) * 0.3F;
        add(p, HEAD, X_ROT, -0.5F, t);
        add(p, RIGHT_ARM, X_ROT, -2.9F + reach, t);
        add(p, RIGHT_ARM, Z_ROT, -0.25F, t);
        add(p, LEFT_ARM, X_ROT, -2.9F - reach, t);
        add(p, LEFT_ARM, Z_ROT, 0.25F, t);
    }

    private static void digging(float[] p, float t, float age) {
        float scoop = Mth.sin(age * 0.5F) * 0.5F;
        lean(p, 0.75F, t, 0.8F);
        add(p, RIGHT_ARM, X_ROT, -0.5F + scoop, t);
        add(p, LEFT_ARM, X_ROT, -0.5F - scoop, t);
        add(p, HEAD, X_ROT, 0.5F, t);
    }

    private static void lean(float[] p, float angle, float t, float armFollow) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float neck = 12.0F - 12.0F * cos;
        put(p, BODY, X_ROT, angle, t);
        put(p, BODY, Y, neck, t);
        put(p, HEAD, Y, neck, t);
        put(p, RIGHT_ARM, Y, neck + 2.0F * cos, t);
        put(p, RIGHT_ARM, Z, 2.0F * sin, t);
        add(p, RIGHT_ARM, X_ROT, angle * armFollow, t);
        put(p, LEFT_ARM, Y, neck + 2.0F * cos, t);
        put(p, LEFT_ARM, Z, 2.0F * sin, t);
        add(p, LEFT_ARM, X_ROT, angle * armFollow, t);
        put(p, RIGHT_LEG, Y, 12.0F, t);
        put(p, RIGHT_LEG, Z, 12.0F * sin, t);
        put(p, LEFT_LEG, Y, 12.0F, t);
        put(p, LEFT_LEG, Z, 12.0F * sin, t);
    }

    private static float get(float[] p, int part, int field) {
        return p[part * STRIDE + field];
    }

    private static void put(float[] p, int part, int field, float value, float t) {
        int i = part * STRIDE + field;
        p[i] += (value - p[i]) * t;
    }

    private static void add(float[] p, int part, int field, float value, float t) {
        p[part * STRIDE + field] += value * t;
    }

    private static final class PlayerDimensions {
        private static final EntityDimensions SITTING = EntityDimensions.scalable(0.6F, 1.2F)
            .withEyeHeight(1.0F)
            .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, Player.DEFAULT_VEHICLE_ATTACHMENT));
        private static final EntityDimensions SLIDING = EntityDimensions.scalable(0.6F, 0.9F)
            .withEyeHeight(0.6F)
            .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, Player.DEFAULT_VEHICLE_ATTACHMENT));
    }
}
