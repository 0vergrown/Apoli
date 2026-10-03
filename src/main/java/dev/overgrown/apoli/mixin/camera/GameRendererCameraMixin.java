package dev.overgrown.apoli.mixin.camera;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.overgrown.apoli.client.camera.CameraController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class GameRendererCameraMixin {

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private double apoli$cameraFov(double original, Camera camera, float partialTick, boolean useFovSetting) {
        if (!useFovSetting) return original;
        return CameraController.fov(original, partialTick);
    }

    @Inject(method = "renderLevel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            shift = At.Shift.AFTER))
    private void apoli$cameraRoll(float partialTick, long nanos, PoseStack pose, CallbackInfo ci) {
        float roll = CameraController.roll();
        if (roll != 0.0F) pose.mulPose(Axis.ZP.rotationDegrees(roll));
    }

    @ModifyExpressionValue(method = "renderItemInHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z", ordinal = 0))
    private boolean apoli$hideHand(boolean original) {
        return original && !CameraController.hidesHand();
    }

    @ModifyExpressionValue(method = "renderItemInHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z", ordinal = 1))
    private boolean apoli$hideScreenEffects(boolean original) {
        return original && !CameraController.isDetached();
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void apoli$cinematicBobView(PoseStack pose, float partialTick, CallbackInfo ci) {
        if (CameraController.suppressesBob()) ci.cancel();
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void apoli$cinematicBobHurt(PoseStack pose, float partialTick, CallbackInfo ci) {
        if (CameraController.suppressesBob()) ci.cancel();
    }
}
