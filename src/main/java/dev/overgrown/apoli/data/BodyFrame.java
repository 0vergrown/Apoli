package dev.overgrown.apoli.data;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class BodyFrame {

    private static final double MODEL_ORIGIN = 1.501;
    private static final double PLAYER_MODEL_SCALE = 0.9375;
    private static final double PIXEL = 1.0 / 16.0;
    private static final double DEG_TO_RAD = Math.PI / 180.0;

    private final float bodyYaw;
    private final float bodyPitch;
    private final float bodyRoll;
    private final double modelScale;
    private final double entityScale;
    private final double offsetY;
    private final double offsetZ;
    private final double yawCos;
    private final double yawSin;
    private final double pitchCos;
    private final double pitchSin;
    private final double rollCos;
    private final double rollSin;

    private BodyFrame(float bodyYaw, float bodyPitch, float bodyRoll, double modelScale, double entityScale,
                      double offsetY, double offsetZ) {
        this.bodyYaw = bodyYaw;
        this.bodyPitch = bodyPitch;
        this.bodyRoll = bodyRoll;
        this.modelScale = modelScale;
        this.entityScale = entityScale;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        double yaw = (180.0 - bodyYaw) * DEG_TO_RAD;
        this.yawCos = Math.cos(yaw);
        this.yawSin = Math.sin(yaw);
        this.pitchCos = Math.cos(bodyPitch);
        this.pitchSin = Math.sin(bodyPitch);
        this.rollCos = Math.cos(bodyRoll);
        this.rollSin = Math.sin(bodyRoll);
    }

    public static BodyFrame of(Entity entity) {
        return of(entity, 1.0F, 0.0F);
    }

    public static BodyFrame of(Entity entity, float partialTick) {
        return of(entity, partialTick, partialTick);
    }

    private static BodyFrame of(Entity entity, float partialTick, float tickOffset) {
        float bodyYaw = entity instanceof LivingEntity living
            ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot)
            : Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float bodyPitch = 0.0F;
        float bodyRoll = 0.0F;
        double offsetY = 0.0;
        double offsetZ = 0.0;
        double modelScale = entity instanceof Player ? PLAYER_MODEL_SCALE : 1.0;
        if (entity instanceof Player player) {
            float swimAmount = player.getSwimAmount(partialTick);
            float xRot = player.getViewXRot(partialTick);
            Pose forced = EntityPoses.forced(player);
            boolean forcedGlide = !player.isFallFlying() && forced == Pose.FALL_FLYING;
            if (player.isFallFlying() || forcedGlide) {
                float ticks = (forcedGlide ? EntityPoses.forcedTicks(player) : player.getFallFlyingTicks()) + tickOffset;
                bodyPitch = Mth.clamp(ticks * ticks / 100.0F, 0.0F, 1.0F) * (-90.0F - xRot) * (float) DEG_TO_RAD;
                bodyRoll = glideRoll(player, partialTick);
            } else if (swimAmount > 0.0F) {
                float target = player.isInWater() ? -90.0F - xRot : -90.0F;
                bodyPitch = Mth.lerp(swimAmount, 0.0F, target) * (float) DEG_TO_RAD;
                if (player.isVisuallySwimming()) {
                    offsetY = -1.0;
                    offsetZ = 0.3;
                }
            } else if (forced != null && EntityPoses.movesBody(forced)) {
                float progress = EntityPoses.progress(player, tickOffset);
                bodyPitch = EntityPoses.bodyPitch(forced) * progress * (float) DEG_TO_RAD;
                offsetY = -EntityPoses.bodyDrop(forced) * progress * PIXEL * modelScale;
            }
        }
        return new BodyFrame(bodyYaw, bodyPitch, bodyRoll, modelScale, entityScale(entity), offsetY, offsetZ);
    }

    private static double entityScale(Entity entity) {
        return 1.0;
    }

    public float bodyYaw() {
        return bodyYaw;
    }

    public void modelToWorld(double x, double y, double z, double[] out) {
        double lx = -x * PIXEL * modelScale;
        double ly = (MODEL_ORIGIN - y * PIXEL) * modelScale;
        double lz = z * PIXEL * modelScale + offsetZ;
        ly += offsetY;
        toWorld(lx, ly, lz, out);
        out[0] *= entityScale;
        out[1] *= entityScale;
        out[2] *= entityScale;
    }

    public void worldToModel(double wx, double wy, double wz, double[] out) {
        double scale = entityScale == 0.0 ? 1.0 : entityScale;
        fromWorld(wx / scale, wy / scale, wz / scale, out);
        double lx = out[0];
        double ly = out[1] - offsetY;
        double lz = out[2] - offsetZ;
        out[0] = -lx / (PIXEL * modelScale);
        out[1] = (MODEL_ORIGIN - ly / modelScale) / PIXEL;
        out[2] = lz / (PIXEL * modelScale);
    }

    public void worldDirectionToModel(double dx, double dy, double dz, double[] out) {
        double scale = entityScale == 0.0 ? 1.0 : entityScale;
        fromWorld(dx / scale, dy / scale, dz / scale, out);
        double factor = PIXEL * modelScale;
        out[0] = -out[0] / factor;
        out[1] = -out[1] / factor;
        out[2] = out[2] / factor;
    }

    public Vec3 localDirectionToWorld(double x, double y, double z) {
        double[] out = new double[3];
        toWorld(x, y, z, out);
        return new Vec3(out[0], out[1], out[2]);
    }

    private void toWorld(double x, double y, double z, double[] out) {
        if (bodyRoll != 0.0F) {
            double nx = x * rollCos + z * rollSin;
            z = -x * rollSin + z * rollCos;
            x = nx;
        }
        if (bodyPitch != 0.0F) {
            double ny = y * pitchCos - z * pitchSin;
            z = y * pitchSin + z * pitchCos;
            y = ny;
        }
        double nx = x * yawCos + z * yawSin;
        z = -x * yawSin + z * yawCos;
        out[0] = nx;
        out[1] = y;
        out[2] = z;
    }

    private void fromWorld(double x, double y, double z, double[] out) {
        double nx = x * yawCos - z * yawSin;
        z = x * yawSin + z * yawCos;
        x = nx;
        if (bodyPitch != 0.0F) {
            double ny = y * pitchCos + z * pitchSin;
            z = -y * pitchSin + z * pitchCos;
            y = ny;
        }
        if (bodyRoll != 0.0F) {
            double rx = x * rollCos - z * rollSin;
            z = x * rollSin + z * rollCos;
            x = rx;
        }
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }

    private static float glideRoll(Player player, float partialTick) {
        Vec3 view = player.getViewVector(partialTick);
        Vec3 motion = player.getDeltaMovement();
        double motionSqr = motion.horizontalDistanceSqr();
        double viewSqr = view.horizontalDistanceSqr();
        if (motionSqr <= 0.0 || viewSqr <= 0.0) return 0.0F;
        double dot = (motion.x * view.x + motion.z * view.z) / Math.sqrt(motionSqr * viewSqr);
        double cross = motion.x * view.z - motion.z * view.x;
        return (float) (Math.signum(cross) * Math.acos(Mth.clamp(dot, -1.0, 1.0)));
    }
}
