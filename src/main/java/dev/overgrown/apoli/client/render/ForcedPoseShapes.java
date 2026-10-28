package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.data.EntityPoses;
import dev.overgrown.apoli.data.HumanoidPose;
import dev.overgrown.apoli.data.PoseMath;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

@Environment(EnvType.CLIENT)
public final class ForcedPoseShapes {

    private static final float[] LIMBS = new float[HumanoidPose.PART_COUNT * PoseMath.STRIDE];

    private ForcedPoseShapes() {}

    public static void apply(HumanoidModel<?> model, LivingEntity entity, Pose pose, float ageInTicks) {
        ModelPartLookup.limbPosesInto(model, LIMBS);
        float progress = EntityPoses.progress(entity, Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F));
        EntityPoses.shape(pose, LIMBS, progress, ageInTicks);
        ModelPartLookup.write(LIMBS, 0, model.head);
        ModelPartLookup.write(LIMBS, PoseMath.STRIDE, model.body);
        ModelPartLookup.write(LIMBS, PoseMath.STRIDE * 2, model.rightArm);
        ModelPartLookup.write(LIMBS, PoseMath.STRIDE * 3, model.leftArm);
        ModelPartLookup.write(LIMBS, PoseMath.STRIDE * 4, model.rightLeg);
        ModelPartLookup.write(LIMBS, PoseMath.STRIDE * 5, model.leftLeg);
        model.hat.copyFrom(model.head);
    }
}
