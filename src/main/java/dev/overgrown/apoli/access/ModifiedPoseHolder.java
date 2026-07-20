package dev.overgrown.apoli.access;

import dev.overgrown.apoli.data.ArmPoseReference;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

public interface ModifiedPoseHolder {

    @Nullable Pose apoli$getModifiedEntityPose();
    void apoli$setModifiedEntityPose(@Nullable Pose entityPose);

    @Nullable ArmPoseReference apoli$getModifiedArmPose(InteractionHand hand);
    void apoli$setModifiedArmPose(@Nullable ArmPoseReference mainHand, @Nullable ArmPoseReference offHand);

    @Nullable Pose apoli$getPreviousEntityPose();
    void apoli$setPreviousEntityPose(@Nullable Pose entityPose);
}
