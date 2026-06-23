package dev.overgrown.apoli.mixin.pose;

import dev.overgrown.apoli.access.ModifiedPoseHolder;
import dev.overgrown.apoli.client.ArmPoseReferenceClient;
import dev.overgrown.apoli.data.ArmPoseReference;
import dev.overgrown.apoli.power.builtin.PosePower;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class PlayerRendererPoseMixin {

    @Inject(method = "getArmPose", at = @At("RETURN"), cancellable = true)
    private static void apoli$overrideArmPose(AbstractClientPlayer player, InteractionHand hand,
                                              CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (player instanceof ModifiedPoseHolder holder) {
            ArmPoseReference ref = holder.apoli$getModifiedArmPose();
            if (ref != null) {
                cir.setReturnValue(ArmPoseReferenceClient.toVanilla(ref));
            }
        }
    }

    @Redirect(method = "setupRotations", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;isFallFlying()Z"))
    private boolean apoli$forceFallFlyingPose(AbstractClientPlayer player) {
        return player.isFallFlying() || PosePower.hasEntityPose(player, Pose.FALL_FLYING);
    }

    @Redirect(method = "setupRotations", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;isAutoSpinAttack()Z"))
    private boolean apoli$forceRiptidePose(AbstractClientPlayer player) {
        return player.isAutoSpinAttack() || PosePower.hasEntityPose(player, Pose.SPIN_ATTACK);
    }
}
