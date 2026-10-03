package dev.overgrown.apoli.mixin.camera;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.camera.CameraController;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class GameRendererCameraMixin {

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private double apoli$cameraFov(double original, Camera camera, float partialTick, boolean useFovSetting) {
        if (!useFovSetting) return original;
        return CameraController.fov(original, partialTick);
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
