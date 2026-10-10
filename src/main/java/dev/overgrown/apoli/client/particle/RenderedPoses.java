package dev.overgrown.apoli.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.LevelRenderPass;
import dev.overgrown.apoli.client.render.ModelPartLookup;
import dev.overgrown.apoli.data.BodyPart;
import dev.overgrown.apoli.data.HumanoidPose;
import dev.overgrown.apoli.data.ModelPartAnchor;
import dev.overgrown.apoli.data.PoseMath;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.Util;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class RenderedPoses {
    private static final long WATCH_MILLIS = 5000L;
    private static final int MAX_AGE_FRAMES = 3;
    private static final int MAX_DEPTH = 4;
    private static final float PIXEL = 1.0F / 16.0F;
    private static final Int2LongOpenHashMap WATCHED = new Int2LongOpenHashMap();
    private static final Int2ObjectOpenHashMap<Pose> POSES = new Int2ObjectOpenHashMap<>();
    private static final Matrix4f[] ROOTS = new Matrix4f[MAX_DEPTH];
    private static final Entity[] ROOT_ENTITIES = new Entity[MAX_DEPTH];
    private static int depth;
    private static int frame;

    static {
        for (int i = 0; i < MAX_DEPTH; i++) ROOTS[i] = new Matrix4f();
    }

    private RenderedPoses() {}

    public static final class Pose {
        private final Matrix4f local = new Matrix4f();
        private final float[] limbs = new float[HumanoidPose.PART_COUNT * PoseMath.STRIDE];
        private double x;
        private double y;
        private double z;
        private int frame;

        @Nullable
        public ModelPartAnchor.Frame frame(BodyPart sided, Vec3 base) {
            if (!sided.hasPoint()) return null;
            Matrix3f basis = new Matrix3f(local);
            Vector3f scale = basis.getScale(new Vector3f());
            if (scale.x < 1.0e-6F || scale.y < 1.0e-6F || scale.z < 1.0e-6F) return null;
            basis.scale(1.0F / scale.x, 1.0F / scale.y, 1.0F / scale.z);
            float[] point = new float[3];
            sided.pointInto(limbs, new double[3], point);
            Vector3f at = local.transformPosition(point[0] * PIXEL, point[1] * PIXEL, point[2] * PIXEL, new Vector3f());
            Vec3 pivot = new Vec3(x + at.x - base.x, y + at.y - base.y, z + at.z - base.z);
            int limb = HumanoidPose.singleLimb(sided);
            if (limb < 0 || sided.isGroup()) return ModelPartAnchor.Frame.world(pivot, 0.0F, 0.0F, 0.0F, basis);
            int offset = limb * PoseMath.STRIDE;
            return ModelPartAnchor.Frame.world(pivot, limbs[offset + PoseMath.X_ROT], limbs[offset + PoseMath.Y_ROT],
                limbs[offset + PoseMath.Z_ROT], basis);
        }
    }

    public static void nextFrame() {
        frame++;
        depth = 0;
        if ((frame & 255) == 0) expire();
    }

    public static void watch(Entity entity) {
        WATCHED.put(entity.getId(), Util.getMillis() + WATCH_MILLIS);
    }

    @Nullable
    public static Pose recent(Entity entity) {
        if (POSES.isEmpty()) return null;
        Pose pose = POSES.get(entity.getId());
        return pose != null && frame - pose.frame <= MAX_AGE_FRAMES ? pose : null;
    }

    public static void begin(Entity entity, PoseStack poseStack) {
        if (depth < MAX_DEPTH) {
            boolean wanted = !WATCHED.isEmpty() && LevelRenderPass.active() && WATCHED.containsKey(entity.getId());
            ROOT_ENTITIES[depth] = wanted ? entity : null;
            if (wanted) ROOTS[depth].set(poseStack.last().pose());
        }
        depth++;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void capture(EntityRenderer renderer, Entity entity, PoseStack poseStack, EntityModel<?> model,
                               float partialTick) {
        int index = depth - 1;
        if (index < 0 || index >= MAX_DEPTH || ROOT_ENTITIES[index] != entity) return;
        if (!(model instanceof HumanoidModel<?> humanoid)) return;
        Pose pose = POSES.get(entity.getId());
        if (pose == null) {
            pose = new Pose();
            POSES.put(entity.getId(), pose);
        }
        pose.local.set(ROOTS[index]).invertAffine().mul(poseStack.last().pose());
        ModelPartLookup.limbPosesInto(humanoid, pose.limbs);
        Vec3 offset = renderer.getRenderOffset(entity, partialTick);
        pose.x = Mth.lerp(partialTick, entity.xOld, entity.getX()) + offset.x;
        pose.y = Mth.lerp(partialTick, entity.yOld, entity.getY()) + offset.y;
        pose.z = Mth.lerp(partialTick, entity.zOld, entity.getZ()) + offset.z;
        pose.frame = frame;
    }

    public static void end() {
        if (depth > 0) depth--;
        if (depth < MAX_DEPTH) ROOT_ENTITIES[depth] = null;
    }

    public static void clear() {
        WATCHED.clear();
        POSES.clear();
        depth = 0;
    }

    private static void expire() {
        if (WATCHED.isEmpty() && POSES.isEmpty()) return;
        long now = Util.getMillis();
        for (ObjectIterator<Int2LongMap.Entry> it = WATCHED.int2LongEntrySet().fastIterator(); it.hasNext(); ) {
            if (it.next().getLongValue() < now) it.remove();
        }
        for (ObjectIterator<Int2ObjectMap.Entry<Pose>> it = POSES.int2ObjectEntrySet().fastIterator(); it.hasNext(); ) {
            if (!WATCHED.containsKey(it.next().getIntKey())) it.remove();
        }
    }
}
