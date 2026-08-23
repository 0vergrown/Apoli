package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyCameraSubmersionPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
@Environment(EnvType.CLIENT)
public abstract class CameraSubmersionMixin {

    @ModifyReturnValue(method = "getFluidInCamera", at = @At("RETURN"))
    private FogType apoli$modifySubmersion(FogType original) {
        return ModifyCameraSubmersionPower.remap(((Camera) (Object) this).getEntity(), original);
    }
}
