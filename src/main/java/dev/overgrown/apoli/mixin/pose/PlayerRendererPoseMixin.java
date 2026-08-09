package dev.overgrown.apoli.mixin.pose;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.access.ModifiedPoseHolder;
import dev.overgrown.apoli.client.ArmPoseReferenceClient;
import dev.overgrown.apoli.client.render.ModelPartAnimator;
import dev.overgrown.apoli.data.ArmPoseReference;
import dev.overgrown.apoli.power.builtin.PosePower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class PlayerRendererPoseMixin {

    private static final String SETUP_ROTATIONS = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V";
    private static final String RENDER_OFFSET = "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)Lnet/minecraft/world/phys/Vec3;";

    @Inject(method = "getArmPose", at = @At("RETURN"), cancellable = true)
    private static void apoli$overrideArmPose(AbstractClientPlayer player, InteractionHand hand,
                                              CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (player instanceof ModifiedPoseHolder holder) {
            ArmPoseReference ref = holder.apoli$getModifiedArmPose(hand);
            if (ref != null) {
                cir.setReturnValue(ArmPoseReferenceClient.toVanilla(ref));
            }
        }
    }

    @Redirect(method = SETUP_ROTATIONS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;isFallFlying()Z"))
    private boolean apoli$forceFallFlyingPose(AbstractClientPlayer player) {
        if (ModelPartAnimator.overridesPose(player)) return false;
        return player.isFallFlying() || PosePower.hasEntityPose(player, Pose.FALL_FLYING);
    }

    @Redirect(method = SETUP_ROTATIONS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;isAutoSpinAttack()Z"))
    private boolean apoli$forceRiptidePose(AbstractClientPlayer player) {
        return player.isAutoSpinAttack() || PosePower.hasEntityPose(player, Pose.SPIN_ATTACK);
    }

    @WrapOperation(method = SETUP_ROTATIONS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSwimAmount(F)F"))
    private float apoli$neutralizeSwimRotation(AbstractClientPlayer player, float partialTick,
                                               Operation<Float> original) {
        return ModelPartAnimator.overridesPose(player) ? 0.0F : original.call(player, partialTick);
    }

    @WrapOperation(method = RENDER_OFFSET, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;isCrouching()Z"))
    private boolean apoli$neutralizeCrouchOffset(AbstractClientPlayer player, Operation<Boolean> original) {
        return !ModelPartAnimator.overridesPose(player) && original.call(player);
    }
}
