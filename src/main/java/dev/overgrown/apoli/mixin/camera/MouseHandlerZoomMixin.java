package dev.overgrown.apoli.mixin.camera;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.client.camera.CameraController;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MouseHandler.class)
@OnlyIn(Dist.CLIENT)
public abstract class MouseHandlerZoomMixin {

    @ModifyExpressionValue(method = "turnPlayer",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;smoothCamera:Z"))
    private boolean apoli$zoomSmoothing(boolean original) {
        return original || CameraController.smoothsMouse();
    }

    @ModifyArg(method = "turnPlayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 0)
    private double apoli$zoomSensitivityYaw(double yaw) {
        return yaw * CameraController.sensitivity();
    }

    @ModifyArg(method = "turnPlayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 1)
    private double apoli$zoomSensitivityPitch(double pitch) {
        return pitch * CameraController.sensitivity();
    }
}
