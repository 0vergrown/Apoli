package dev.overgrown.apoli.data;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class ModelPartAnchor {

    private static final float MODEL_ORIGIN = 1.501F;
    private static final float PLAYER_MODEL_SCALE = 0.9375F;
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private ModelPartAnchor() {}

    public static final class Frame {
        private final Vec3 pivot;
        private final float partXRot;
        private final float partYRot;
        private final float partZRot;
        private final float bodyYaw;
        private final float bodyPitch;
        private final float bodyRoll;

        Frame(Vec3 pivot, float partXRot, float partYRot, float partZRot,
              float bodyYaw, float bodyPitch, float bodyRoll) {
            this.pivot = pivot;
            this.partXRot = partXRot;
            this.partYRot = partYRot;
            this.partZRot = partZRot;
            this.bodyYaw = bodyYaw;
            this.bodyPitch = bodyPitch;
            this.bodyRoll = bodyRoll;
        }

        public Vec3 pivot() {
            return pivot;
        }

        public Vec3 direction(Vec3 local) {
            if (local.lengthSqr() < 1.0e-12) return Vec3.ZERO;
            Vec3 model = rotate(new Vec3(local.x, -local.y, -local.z), partXRot, partYRot, partZRot);
            return body(new Vec3(-model.x, -model.y, model.z), bodyYaw, bodyPitch, bodyRoll);
        }
    }

    public static Frame frameOf(Entity entity, String normalizedPart) {
        float bodyYaw = bodyYaw(entity);
        float bodyPitch = 0.0F;
        float bodyRoll = 0.0F;
        boolean crawling = false;
        if (entity instanceof Player player) {
            float swimAmount = player.getSwimAmount(1.0F);
            if (player.isFallFlying()) {
                float ticks = player.getFallFlyingTicks();
                bodyPitch = Mth.clamp(ticks * ticks / 100.0F, 0.0F, 1.0F) * (-90.0F - player.getXRot()) * DEG_TO_RAD;
                bodyRoll = glideRoll(player);
            } else if (swimAmount > 0.0F) {
                float target = player.isInWater() ? -90.0F - player.getXRot() : -90.0F;
                bodyPitch = Mth.lerp(swimAmount, 0.0F, target) * DEG_TO_RAD;
                crawling = player.isVisuallySwimming();
            }
        }

        int part = anchorPart(entity, normalizedPart);
        if (part < 0) {
            Vec3 centre = new Vec3(0.0, entity.getBbHeight() * 0.5, 0.0);
            return new Frame(centre, 0.0F, 0.0F, 0.0F, bodyYaw, bodyPitch, bodyRoll);
        }

        HumanoidPose pose = HumanoidPose.of(entity);
        float x = pose.x(part);
        float y = pose.y(part);
        float z = pose.z(part);
        float extent = extent(normalizedPart) * pose.yScale(part);
        if (extent != 0.0F) {
            Vec3 tip = rotate(new Vec3(0.0, extent, 0.0), pose.xRot(part), pose.yRot(part), pose.zRot(part));
            x += (float) tip.x;
            y += (float) tip.y;
            z += (float) tip.z;
        }

        float modelScale = entity instanceof Player ? PLAYER_MODEL_SCALE : 1.0F;
        Vec3 local = new Vec3(
            -x * PIXEL * modelScale,
            (MODEL_ORIGIN - y * PIXEL) * modelScale,
            z * PIXEL * modelScale);
        if (crawling) local = local.add(0.0, -1.0, 0.3);
        Vec3 pivot = body(local, bodyYaw, bodyPitch, bodyRoll).scale(entityScale(entity));
        return new Frame(pivot, pose.xRot(part), pose.yRot(part), pose.zRot(part), bodyYaw, bodyPitch, bodyRoll);
    }

    public static Vec3 offsetOf(Entity entity, String normalizedPart) {
        return frameOf(entity, normalizedPart).pivot();
    }

    private static Vec3 body(Vec3 v, float bodyYaw, float bodyPitch, float bodyRoll) {
        Vec3 out = v;
        if (bodyRoll != 0.0F) out = rotate(out, 0.0F, bodyRoll, 0.0F);
        if (bodyPitch != 0.0F) out = rotate(out, bodyPitch, 0.0F, 0.0F);
        return rotate(out, 0.0F, (180.0F - bodyYaw) * DEG_TO_RAD, 0.0F);
    }

    private static float glideRoll(Player player) {
        Vec3 view = player.getViewVector(1.0F);
        Vec3 motion = player.getDeltaMovement();
        double motionSqr = motion.horizontalDistanceSqr();
        double viewSqr = view.horizontalDistanceSqr();
        if (motionSqr <= 0.0 || viewSqr <= 0.0) return 0.0F;
        double dot = (motion.x * view.x + motion.z * view.z) / Math.sqrt(motionSqr * viewSqr);
        double cross = motion.x * view.z - motion.z * view.x;
        return (float) (Math.signum(cross) * Math.acos(Mth.clamp(dot, -1.0, 1.0)));
    }

    private static int anchorPart(Entity entity, String normalizedPart) {
        return switch (normalizedPart) {
            case "righthand", "handright", "rightfist" -> HumanoidPose.RIGHT_ARM;
            case "lefthand", "handleft", "leftfist" -> HumanoidPose.LEFT_ARM;
            case "rightfoot", "footright" -> HumanoidPose.RIGHT_LEG;
            case "leftfoot", "footleft" -> HumanoidPose.LEFT_LEG;
            case "mainhand", "handmain" -> mainArm(entity) == HumanoidArm.RIGHT
                ? HumanoidPose.RIGHT_ARM : HumanoidPose.LEFT_ARM;
            case "offhand", "handoff" -> mainArm(entity) == HumanoidArm.RIGHT
                ? HumanoidPose.LEFT_ARM : HumanoidPose.RIGHT_ARM;
            default -> HumanoidPose.indexOf(normalizedPart);
        };
    }

    private static float extent(String normalizedPart) {
        return switch (normalizedPart) {
            case "righthand", "handright", "rightfist", "lefthand", "handleft", "leftfist",
                 "mainhand", "handmain", "offhand", "handoff" -> HumanoidPose.ARM_LENGTH;
            case "rightfoot", "footright", "leftfoot", "footleft" -> HumanoidPose.LEG_LENGTH;
            default -> 0.0F;
        };
    }

    private static HumanoidArm mainArm(Entity entity) {
        return entity instanceof LivingEntity living ? living.getMainArm() : HumanoidArm.RIGHT;
    }

    private static float entityScale(Entity entity) {
        return 1.0F;
    }

    private static float bodyYaw(Entity entity) {
        return entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
    }

    static Vec3 rotate(Vec3 v, float xRot, float yRot, float zRot) {
        double x = v.x;
        double y = v.y;
        double z = v.z;
        if (xRot != 0.0F) {
            double cos = Math.cos(xRot);
            double sin = Math.sin(xRot);
            double ny = y * cos - z * sin;
            z = y * sin + z * cos;
            y = ny;
        }
        if (yRot != 0.0F) {
            double cos = Math.cos(yRot);
            double sin = Math.sin(yRot);
            double nx = x * cos + z * sin;
            z = -x * sin + z * cos;
            x = nx;
        }
        if (zRot != 0.0F) {
            double cos = Math.cos(zRot);
            double sin = Math.sin(zRot);
            double nx = x * cos - y * sin;
            y = x * sin + y * cos;
            x = nx;
        }
        return new Vec3(x, y, z);
    }
}
