package dev.overgrown.apoli.mixin.pose;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.access.ModifiedPoseHolder;
import dev.overgrown.apoli.client.ArmPoseReferenceClient;
import dev.overgrown.apoli.client.render.ModelPartAnimator;
import dev.overgrown.apoli.data.ArmPoseReference;
import dev.overgrown.apoli.data.EntityPoses;
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

@Mixin(PlayerRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class PlayerRendererPoseMixin {

    private static final String SETUP_ROTATIONS = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V";
    private static final String RENDER_OFFSET = "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)Lnet/minecraft/world/phys/Vec3;";

    @ModifyReturnValue(method = "getArmPose", at = @At("RETURN"))
    private static HumanoidModel.ArmPose apoli$overrideArmPose(HumanoidModel.ArmPose original, AbstractClientPlayer player,
                                                               InteractionHand hand) {
        if (!(player instanceof ModifiedPoseHolder holder)) return original;
        ArmPoseReference ref = holder.apoli$getModifiedArmPose(hand);
        return ref != null ? ArmPoseReferenceClient.toVanilla(ref) : original;
    }

    @ModifyExpressionValue(method = SETUP_ROTATIONS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;isFallFlying()Z"))
    private boolean apoli$forceFallFlyingPose(boolean original, @Local(argsOnly = true) AbstractClientPlayer player) {
        if (ModelPartAnimator.overridesPose(player)) return false;
        return original || PosePower.hasEntityPose(player, Pose.FALL_FLYING);
    }

    @ModifyExpressionValue(method = SETUP_ROTATIONS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;getFallFlyingTicks()I"))
    private int apoli$forcedFallFlyingTicks(int original, @Local(argsOnly = true) AbstractClientPlayer player) {
        return EntityPoses.fallFlyingTicks(player, original);
    }

    @ModifyExpressionValue(method = SETUP_ROTATIONS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;isAutoSpinAttack()Z"))
    private boolean apoli$forceRiptidePose(boolean original, @Local(argsOnly = true) AbstractClientPlayer player) {
        return original || PosePower.hasEntityPose(player, Pose.SPIN_ATTACK);
    }

    @ModifyExpressionValue(method = SETUP_ROTATIONS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSwimAmount(F)F"))
    private float apoli$neutralizeSwimRotation(float original, @Local(argsOnly = true) AbstractClientPlayer player) {
        return ModelPartAnimator.overridesPose(player) ? 0.0F : original;
    }

    @ModifyExpressionValue(method = RENDER_OFFSET, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;isCrouching()Z"))
    private boolean apoli$neutralizeCrouchOffset(boolean original, @Local(argsOnly = true) AbstractClientPlayer player) {
        return original && !ModelPartAnimator.overridesPose(player);
    }
}
