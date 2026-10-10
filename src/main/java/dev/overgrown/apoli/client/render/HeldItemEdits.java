package dev.overgrown.apoli.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.data.BodyAttachments;
import dev.overgrown.apoli.data.ModelPartTimeline;
import dev.overgrown.apoli.data.ModelPartTransformation;
import dev.overgrown.apoli.data.PoseMath;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;

public final class HeldItemEdits {
    private HeldItemEdits() {}

    public static final int NONE = 0;
    public static final int EDITED = 1;
    public static final int HIDDEN = 2;

    private static final float[] POSE = new float[PoseMath.STRIDE];
    private static final Quaternionf ROTATION = new Quaternionf();

    public static int resolve(@Nullable LivingEntity entity, ItemDisplayContext context, boolean leftHand) {
        if (entity == null || !isHand(context)) return NONE;
        List<ModelPartTimeline.Slot> slots = ModelPartAnimator.update(entity);
        if (slots.isEmpty()) return NONE;
        int hand = leftHand ? BodyAttachments.LEFT_ITEM : BodyAttachments.RIGHT_ITEM;
        boolean firstPerson = context.firstPerson() || FirstPersonHeldItem.drawing();
        boolean edited = false;
        boolean hidden = false;
        float[] pose = POSE;
        for (int i = 0; i < slots.size(); i++) {
            ModelPartTimeline.Slot slot = slots.get(i);
            float weight = slot.weight();
            if (weight <= 0.0F || !slot.rendersIn(firstPerson)) continue;
            ModelPartTransformation transformation = slot.transformation();
            if ((transformation.bodyPart().sided(entity).attachments() & hand) == 0) continue;
            if (!edited) {
                reset(pose);
                edited = true;
            }
            float value = slot.value();
            boolean override = transformation.overrideAnimation();
            switch (transformation.type()) {
                case PITCH -> pose[PoseMath.X_ROT] = rotate(pose[PoseMath.X_ROT], value, weight, override);
                case YAW -> pose[PoseMath.Y_ROT] = rotate(pose[PoseMath.Y_ROT], value, weight, override);
                case ROLL -> pose[PoseMath.Z_ROT] = rotate(pose[PoseMath.Z_ROT], value, weight, override);
                case X_SCALE -> pose[PoseMath.X_SCALE] = 1.0F + value * weight;
                case Y_SCALE -> pose[PoseMath.Y_SCALE] = 1.0F + value * weight;
                case Z_SCALE -> pose[PoseMath.Z_SCALE] = 1.0F + value * weight;
                case PIVOT_X -> pose[PoseMath.X] += value * weight;
                case PIVOT_Y -> pose[PoseMath.Y] += value * weight;
                case PIVOT_Z -> pose[PoseMath.Z] += value * weight;
                case VISIBLE -> {
                    if (weight >= 0.5F) hidden = value == 0.0F;
                }
                case HIDDEN -> {
                    if (weight >= 0.5F) hidden = value != 0.0F;
                }
                default -> {
                }
            }
        }
        if (hidden) return HIDDEN;
        return edited ? EDITED : NONE;
    }

    public static void apply(PoseStack poseStack) {
        float[] pose = POSE;
        if (pose[PoseMath.X] != 0.0F || pose[PoseMath.Y] != 0.0F || pose[PoseMath.Z] != 0.0F) {
            poseStack.translate(pose[PoseMath.X] / 16.0F, pose[PoseMath.Y] / 16.0F, pose[PoseMath.Z] / 16.0F);
        }
        if (pose[PoseMath.X_ROT] != 0.0F || pose[PoseMath.Y_ROT] != 0.0F || pose[PoseMath.Z_ROT] != 0.0F) {
            poseStack.mulPose(ROTATION.rotationZYX(pose[PoseMath.Z_ROT], pose[PoseMath.Y_ROT], pose[PoseMath.X_ROT]));
        }
        if (pose[PoseMath.X_SCALE] != 1.0F || pose[PoseMath.Y_SCALE] != 1.0F || pose[PoseMath.Z_SCALE] != 1.0F) {
            poseStack.scale(pose[PoseMath.X_SCALE], pose[PoseMath.Y_SCALE], pose[PoseMath.Z_SCALE]);
        }
    }

    private static boolean isHand(ItemDisplayContext context) {
        return context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }

    private static float rotate(float current, float value, float weight, boolean override) {
        return override ? current + (value - current) * weight : current + value * weight;
    }

    private static void reset(float[] pose) {
        pose[PoseMath.X] = 0.0F;
        pose[PoseMath.Y] = 0.0F;
        pose[PoseMath.Z] = 0.0F;
        pose[PoseMath.X_ROT] = 0.0F;
        pose[PoseMath.Y_ROT] = 0.0F;
        pose[PoseMath.Z_ROT] = 0.0F;
        pose[PoseMath.X_SCALE] = 1.0F;
        pose[PoseMath.Y_SCALE] = 1.0F;
        pose[PoseMath.Z_SCALE] = 1.0F;
    }
}
