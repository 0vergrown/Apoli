package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ScaleRender {
    private ScaleRender() {}

    public static final float MATTRESS_DROP = 0.125F;

    private static boolean doll;

    public static void beginDoll() {
        doll = true;
    }

    public static void endDoll() {
        doll = false;
    }

    public static boolean inDoll() {
        return doll;
    }

    public static float modelWidth(Entity entity, float partialTick) {
        return Scales.appliedIfScaled(entity, doll ? ScaleTypes.DOLL : ScaleTypes.MODEL_WIDTH, partialTick);
    }

    public static float modelHeight(Entity entity, float partialTick) {
        return Scales.appliedIfScaled(entity, doll ? ScaleTypes.DOLL : ScaleTypes.MODEL_HEIGHT, partialTick);
    }

    public static boolean lying(LivingEntity entity) {
        return entity.hasPose(Pose.SLEEPING) && !ModelPartAnimator.overridesPose(entity);
    }

    public static void undoOuterScale(Entity entity, PoseStack poseStack, float partialTick) {
        float width = modelWidth(entity, partialTick);
        float height = modelHeight(entity, partialTick);
        if (width != 1.0F || height != 1.0F) poseStack.scale(1.0F / width, 1.0F / height, 1.0F / width);
    }

    public static @Nullable Vec3 nameTagAnchor(Entity entity, @Nullable Vec3 anchor, float partialTick) {
        if (anchor == null || doll || Scales.untouched(entity)) return anchor;
        float across = Scales.applied(entity, ScaleTypes.MODEL_WIDTH, partialTick)
            / Scales.applied(entity, ScaleTypes.HITBOX_WIDTH);
        float up = Scales.applied(entity, ScaleTypes.MODEL_HEIGHT, partialTick)
            / Scales.applied(entity, ScaleTypes.HITBOX_HEIGHT);
        if (across == 1.0F && up == 1.0F) return anchor;
        return new Vec3(anchor.x * across, anchor.y * up, anchor.z * across);
    }

    public static Vec3 crouchOffset(Entity entity, Vec3 offset, float partialTick) {
        if (offset.y == 0.0) return offset;
        float height = modelHeight(entity, partialTick);
        return height == 1.0F ? offset : new Vec3(offset.x, offset.y * height, offset.z);
    }

    public static float dollHeight(LivingEntity entity, float height) {
        if (Scales.untouched(entity)) return height;
        return height / Scales.applied(entity, ScaleTypes.HITBOX_HEIGHT) * Scales.applied(entity, ScaleTypes.DOLL);
    }
}
