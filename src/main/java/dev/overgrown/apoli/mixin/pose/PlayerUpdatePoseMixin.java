package dev.overgrown.apoli.mixin.pose;

import dev.overgrown.apoli.access.ModifiedPoseHolder;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerUpdatePoseMixin {

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void apoli$preventUpdatingPose(CallbackInfo ci) {
        if (((ModifiedPoseHolder) (Object) this).apoli$getModifiedEntityPose() != null) {
            ci.cancel();
        }
    }
}
