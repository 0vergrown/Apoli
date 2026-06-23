package dev.overgrown.apoli.mixin.pose;

import dev.overgrown.apoli.access.ModifiedPoseHolder;
import dev.overgrown.apoli.data.ArmPoseReference;
import dev.overgrown.apoli.power.builtin.PosePower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPoseMixin implements ModifiedPoseHolder {

    @Unique private Pose apoli$modifiedEntityPose;
    @Unique private ArmPoseReference apoli$modifiedArmPose;
    @Unique private Pose apoli$previousEntityPose;

    @Override public Pose apoli$getModifiedEntityPose() { return apoli$modifiedEntityPose; }
    @Override public void apoli$setModifiedEntityPose(Pose entityPose) { this.apoli$modifiedEntityPose = entityPose; }

    @Override public ArmPoseReference apoli$getModifiedArmPose() { return apoli$modifiedArmPose; }
    @Override public void apoli$setModifiedArmPose(ArmPoseReference armPose) { this.apoli$modifiedArmPose = armPose; }

    @Override public Pose apoli$getPreviousEntityPose() { return apoli$previousEntityPose; }
    @Override public void apoli$setPreviousEntityPose(Pose entityPose) { this.apoli$previousEntityPose = entityPose; }

    @Inject(method = "tick", at = @At("TAIL"))
    private void apoli$applyPose(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        PosePower.Config chosen = PosePower.chooseBest(self);

        if (chosen != null) {
            if (!(self instanceof Player) && apoli$previousEntityPose == null && chosen.entityPose().isPresent()) {
                apoli$previousEntityPose = self.getPose();
            }
            apoli$modifiedEntityPose = chosen.entityPose().orElse(null);
            apoli$modifiedArmPose = chosen.armPose().orElse(null);
            chosen.entityPose().ifPresent(self::setPose);
        } else {
            apoli$modifiedEntityPose = null;
            apoli$modifiedArmPose = null;
            if (!(self instanceof Player) && apoli$previousEntityPose != null) {
                self.setPose(apoli$previousEntityPose);
                apoli$previousEntityPose = null;
            }
        }
    }
}
