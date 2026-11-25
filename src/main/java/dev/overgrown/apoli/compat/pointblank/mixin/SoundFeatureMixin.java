package dev.overgrown.apoli.compat.pointblank.mixin;

import com.vicmatskiv.pointblank.feature.SoundFeature;
import dev.overgrown.apoli.compat.gun.GunPowers;
import dev.overgrown.apoli.compat.pointblank.PointBlankGuns;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundFeature.class)
public abstract class SoundFeatureMixin {

    @Inject(method = "playFireSound", at = @At("HEAD"))
    private static void apoli$gunFired(Player player, ItemStack itemStack, CallbackInfo ci) {
        if (player == null || player.level().isClientSide()) return;
        GunPowers.onFire(player, itemStack, PointBlankGuns.gunId(itemStack));
    }
}
