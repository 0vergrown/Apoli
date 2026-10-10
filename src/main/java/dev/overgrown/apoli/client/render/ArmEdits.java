package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.data.BodyPart;
import dev.overgrown.apoli.data.ModelPartTimeline;
import dev.overgrown.apoli.data.ModelPartTransformation;
import dev.overgrown.apoli.data.PoseMath;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class ArmEdits {
    private ArmEdits() {}

    private static final int RIGHT_EDITED = 1;
    private static final int RIGHT_HELD = 1 << 1;
    private static final int RIGHT_REACHING = 1 << 2;
    private static final int LEFT_EDITED = 1 << 3;
    private static final int LEFT_HELD = 1 << 4;
    private static final int LEFT_REACHING = 1 << 5;

    public static int of(LivingEntity entity, boolean firstPerson) {
        List<ModelPartTimeline.Slot> slots = ModelPartAnimator.update(entity);
        if (slots.isEmpty()) return 0;
        int state = 0;
        for (int i = 0; i < slots.size(); i++) {
            ModelPartTimeline.Slot slot = slots.get(i);
            float weight = slot.weight();
            if (weight <= 0.0F || !slot.rendersIn(firstPerson)) continue;
            ModelPartTransformation transformation = slot.transformation();
            if (!PoseMath.isSpatial(transformation.type())) continue;
            BodyPart part = transformation.bodyPart().sided(entity);
            if (part.models() == 0) continue;
            int limbs = part.limbs();
            boolean right = (limbs & BodyPart.LIMB_RIGHT_ARM) != 0;
            boolean left = (limbs & BodyPart.LIMB_LEFT_ARM) != 0;
            if (!right && !left) continue;
            boolean reaching = transformation.type() == ModelPartTransformation.Type.REACH;
            boolean held = reaching
                && (transformation.overrideAnimation() || !firstPerson)
                && Mth.clamp(slot.value(), 0.0F, 1.0F) * weight > 0.0F;
            if (right) state |= RIGHT_EDITED | (reaching ? RIGHT_REACHING : 0) | (held ? RIGHT_HELD : 0);
            if (left) state |= LEFT_EDITED | (reaching ? LEFT_REACHING : 0) | (held ? LEFT_HELD : 0);
        }
        return state;
    }

    public static boolean edited(int state, HumanoidArm arm) {
        return (state & (arm == HumanoidArm.RIGHT ? RIGHT_EDITED : LEFT_EDITED)) != 0;
    }

    public static boolean held(int state, HumanoidArm arm) {
        return (state & (arm == HumanoidArm.RIGHT ? RIGHT_HELD : LEFT_HELD)) != 0;
    }

    public static boolean reaching(int state, HumanoidArm arm) {
        return (state & (arm == HumanoidArm.RIGHT ? RIGHT_REACHING : LEFT_REACHING)) != 0;
    }

    public static HumanoidArm armFor(LivingEntity entity, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
    }
}
