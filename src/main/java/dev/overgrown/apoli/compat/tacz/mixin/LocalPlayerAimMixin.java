package dev.overgrown.apoli.compat.tacz.mixin;

import com.tacz.guns.client.gameplay.LocalPlayerAim;
import dev.overgrown.apoli.compat.gun.GunPowers;
import dev.overgrown.apoli.compat.tacz.TaczGuns;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayerAim.class)
public abstract class LocalPlayerAimMixin {

    @Shadow @Final private LocalPlayer player;

    @Shadow public abstract void aim(boolean isAim);

    @Shadow public abstract boolean isAim();

    @ModifyVariable(method = "aim", at = @At("HEAD"), argsOnly = true)
    private boolean apoli$preventAim(boolean isAim) {
        return isAim && !GunPowers.prevents(player, GunPowers.PREVENT_AIM, TaczGuns.gunId(player.getMainHandItem()));
    }

    @Inject(method = "tickAimingProgress", at = @At("HEAD"))
    private void apoli$releaseAim(CallbackInfo ci) {
        if (isAim() && GunPowers.prevents(player, GunPowers.PREVENT_AIM, TaczGuns.gunId(player.getMainHandItem()))) aim(false);
    }
}
