package dev.overgrown.apoli.compat.pointblank.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.client.controller.PlayerRecoilController;
import dev.overgrown.apoli.compat.gun.GunPowers;
import dev.overgrown.apoli.compat.pointblank.PointBlankGuns;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerRecoilController.class)
public abstract class PlayerRecoilControllerMixin {

    @ModifyExpressionValue(method = "onStartFiring", at = @At(value = "INVOKE",
        target = "Lcom/vicmatskiv/pointblank/feature/RecoilFeature;getRecoilModifier(Lnet/minecraft/world/item/ItemStack;)F"))
    private float apoli$modifyRecoil(float modifier, LivingEntity player, GunClientState state, ItemStack itemStack) {
        return (float) GunPowers.modifyStat(player, GunPowers.MODIFY_GUN_RECOIL, PointBlankGuns.gunId(itemStack), modifier);
    }
}
