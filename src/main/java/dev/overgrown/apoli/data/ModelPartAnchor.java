package dev.overgrown.apoli.data;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class ModelPartAnchor {

    private ModelPartAnchor() {}

    public static final class Frame {
        private final Vec3 pivot;
        private final float partXRot;
        private final float partYRot;
        private final float partZRot;
        @Nullable
        private final BodyFrame body;
        @Nullable
        private final Matrix3f basis;

        Frame(Vec3 pivot, float partXRot, float partYRot, float partZRot, BodyFrame body) {
            this(pivot, partXRot, partYRot, partZRot, body, null);
        }

        private Frame(Vec3 pivot, float partXRot, float partYRot, float partZRot, @Nullable BodyFrame body,
                      @Nullable Matrix3f basis) {
            this.pivot = pivot;
            this.partXRot = partXRot;
            this.partYRot = partYRot;
            this.partZRot = partZRot;
            this.body = body;
            this.basis = basis;
        }

        public static Frame world(Vec3 pivot, float partXRot, float partYRot, float partZRot, Matrix3f basis) {
            return new Frame(pivot, partXRot, partYRot, partZRot, null, basis);
        }

        public Vec3 pivot() {
            return pivot;
        }

        public Vec3 direction(Vec3 local) {
            if (local.lengthSqr() < 1.0e-12) return Vec3.ZERO;
            Vec3 model = rotate(new Vec3(local.x, -local.y, -local.z), partXRot, partYRot, partZRot);
            if (basis != null) {
                Vector3f world = basis.transform((float) model.x, (float) model.y, (float) model.z, new Vector3f());
                return new Vec3(world.x, world.y, world.z);
            }
            return body.localDirectionToWorld(-model.x, -model.y, model.z);
        }
    }

    public static Frame frameOf(Entity entity, BodyPart part) {
        BodyPart sided = part.sided(entity);
        BodyFrame body = BodyFrame.of(entity);
        if (!sided.hasPoint()) return centre(entity, body);
        return frameOf(sided, HumanoidPose.of(entity), body);
    }

    public static Frame frameOf(Entity entity, BodyPart part, ModelPartTimeline timeline, double now,
                                float partialTick) {
        BodyPart sided = part.sided(entity);
        BodyFrame body = BodyFrame.of(entity, partialTick);
        if (!sided.hasPoint()) return centre(entity, body);
        return frameOf(sided, HumanoidPose.of(entity, timeline, now, partialTick), body);
    }

    private static Frame centre(Entity entity, BodyFrame body) {
        return new Frame(new Vec3(0.0, entity.getBbHeight() * 0.5, 0.0), 0.0F, 0.0F, 0.0F, body);
    }

    private static Frame frameOf(BodyPart sided, HumanoidPose pose, BodyFrame body) {
        float[] point = new float[3];
        double[] scratch = new double[3];
        sided.pointInto(pose.poses(), scratch, point);
        body.modelToWorld(point[0], point[1], point[2], scratch);
        Vec3 pivot = new Vec3(scratch[0], scratch[1], scratch[2]);

        int limb = HumanoidPose.singleLimb(sided);
        if (limb < 0 || sided.isGroup()) {
            return new Frame(pivot, 0.0F, 0.0F, 0.0F, body);
        }
        return new Frame(pivot, pose.xRot(limb), pose.yRot(limb), pose.zRot(limb), body);
    }

    public static Vec3 offsetOf(Entity entity, BodyPart part) {
        return frameOf(entity, part).pivot();
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
