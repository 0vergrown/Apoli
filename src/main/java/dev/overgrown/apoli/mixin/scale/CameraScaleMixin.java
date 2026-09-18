package dev.overgrown.apoli.mixin.scale;

import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
@Environment(EnvType.CLIENT)
public abstract class CameraScaleMixin {

    @Shadow
    private Entity entity;

    @ModifyArg(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
               at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"))
    private double apoli$scaleThirdPersonDistance(double distance) {
        Entity viewed = this.entity;
        if (viewed == null) return distance;
        if (Scales.untouched(viewed)) return distance;
        return distance * Scales.applied(viewed, ScaleTypes.THIRD_PERSON);
    }
}
