package dev.overgrown.apoli.compat.pointblank.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.vicmatskiv.pointblank.client.GunClientState;
import dev.overgrown.apoli.compat.gun.GunPowers;
import dev.overgrown.apoli.compat.pointblank.PointBlankGuns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GunClientState.class)
public abstract class GunClientStateMixin {

    @Inject(method = "tryFire", at = @At("HEAD"), cancellable = true)
    private void apoli$preventGunFire(LivingEntity player, ItemStack itemStack, Entity targetEntity,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (GunPowers.prevents(player, GunPowers.PREVENT_GUN_FIRE, PointBlankGuns.gunId(itemStack))) cir.setReturnValue(false);
    }

    @ModifyExpressionValue(method = "getFireCooldownDuration", at = @At(value = "INVOKE",
        target = "Lcom/vicmatskiv/pointblank/feature/FireModeFeature;getRpm(Lnet/minecraft/world/item/ItemStack;)I"))
    private int apoli$modifyFireRate(int rpm) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return rpm;
        double modified = GunPowers.modifyStat(player, GunPowers.MODIFY_GUN_FIRE_RATE, PointBlankGuns.gunId(player.getMainHandItem()), rpm);
        return Math.max(1, (int) Math.round(modified));
    }
}
