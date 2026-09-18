package dev.overgrown.apoli.mixin.tick;

import dev.overgrown.apoli.client.ClientTickRates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
@Environment(EnvType.CLIENT)
public abstract class CameraTickRateMixin {

    @ModifyVariable(
        method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float apoli$slowedCameraPartial(float partialTick) {
        return ClientTickRates.cameraPartial(partialTick);
    }
}
