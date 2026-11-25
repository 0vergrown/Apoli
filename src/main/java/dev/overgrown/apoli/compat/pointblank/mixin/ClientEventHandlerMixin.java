package dev.overgrown.apoli.compat.pointblank.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.vicmatskiv.pointblank.client.ClientEventHandler;
import dev.overgrown.apoli.compat.gun.GunPowers;
import dev.overgrown.apoli.compat.pointblank.PointBlankGuns;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientEventHandler.class)
public abstract class ClientEventHandlerMixin {

    @ModifyVariable(method = "toggleAiming(Lnet/minecraft/client/player/LocalPlayer;Lcom/vicmatskiv/pointblank/item/GunItem$OperableGunContext;Z)Z",
        at = @At("HEAD"), argsOnly = true)
    private boolean apoli$preventAim(boolean isAiming, @Local(argsOnly = true) LocalPlayer player) {
        if (!isAiming || player == null) return isAiming;
        return !GunPowers.prevents(player, GunPowers.PREVENT_AIM, PointBlankGuns.gunId(player.getMainHandItem()));
    }
}
