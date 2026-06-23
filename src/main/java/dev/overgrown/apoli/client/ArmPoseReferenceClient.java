package dev.overgrown.apoli.client;

import dev.overgrown.apoli.data.ArmPoseReference;
import net.minecraft.client.model.HumanoidModel;

public final class ArmPoseReferenceClient {
    private ArmPoseReferenceClient() {}

    public static HumanoidModel.ArmPose toVanilla(ArmPoseReference ref) {
        return switch (ref) {
            case EMPTY -> HumanoidModel.ArmPose.EMPTY;
            case ITEM -> HumanoidModel.ArmPose.ITEM;
            case BLOCK -> HumanoidModel.ArmPose.BLOCK;
            case BOW_AND_ARROW -> HumanoidModel.ArmPose.BOW_AND_ARROW;
            case THROW_SPEAR -> HumanoidModel.ArmPose.THROW_SPEAR;
            case CROSSBOW_CHARGE -> HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            case CROSSBOW_HOLD -> HumanoidModel.ArmPose.CROSSBOW_HOLD;
            case SPYGLASS -> HumanoidModel.ArmPose.SPYGLASS;
            case TOOT_HORN -> HumanoidModel.ArmPose.TOOT_HORN;
            case BRUSH -> HumanoidModel.ArmPose.BRUSH;
        };
    }
}
