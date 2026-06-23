package dev.overgrown.apoli.access;

import dev.overgrown.apoli.data.ArmPoseReference;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

public interface ModifiedPoseHolder {

    @Nullable Pose apoli$getModifiedEntityPose();
    void apoli$setModifiedEntityPose(@Nullable Pose entityPose);

    @Nullable ArmPoseReference apoli$getModifiedArmPose();
    void apoli$setModifiedArmPose(@Nullable ArmPoseReference armPose);

    @Nullable Pose apoli$getPreviousEntityPose();
    void apoli$setPreviousEntityPose(@Nullable Pose entityPose);
}
