package dev.overgrown.apoli.compat.tacz.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.client.event.CameraSetupEvent;
import dev.overgrown.apoli.compat.gun.GunPowers;
import dev.overgrown.apoli.compat.tacz.TaczGuns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CameraSetupEvent.class)
public abstract class CameraSetupEventMixin {

    @ModifyExpressionValue(method = "initialCameraRecoil", at = @At(value = "INVOKE",
        target = "Lcom/tacz/guns/api/modifier/ParameterizedCache;eval(D)D"))
    private static double apoli$modifyRecoil(double recoil, GunFireEvent event) {
        return GunPowers.modifyStat(event.getShooter(), GunPowers.MODIFY_GUN_RECOIL, TaczGuns.gunId(event.getGunItemStack()), recoil);
    }
}
