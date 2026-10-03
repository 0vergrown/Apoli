package dev.overgrown.apoli.mixin.camera;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.client.camera.CameraController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
@Environment(EnvType.CLIENT)
public abstract class GuiCrosshairCameraMixin {
    @ModifyExpressionValue(method = "renderCrosshair",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
    private boolean apoli$hideDetachedCrosshair(boolean original) {
        return original && !CameraController.isDetached();
    }
}
