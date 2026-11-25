package dev.overgrown.apoli.compat.pointblank.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.vicmatskiv.pointblank.item.HurtingItem;
import dev.overgrown.apoli.compat.gun.GunHit;
import dev.overgrown.apoli.compat.gun.GunPowers;
import dev.overgrown.apoli.compat.pointblank.PointBlankGuns;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HurtingItem.class)
public abstract class HurtingItemMixin {

    @ModifyExpressionValue(method = "hurtEntity", at = @At(value = "INVOKE",
        target = "Lcom/vicmatskiv/pointblank/feature/FireModeFeature;getDamage(Lnet/minecraft/world/item/ItemStack;)F"))
    private float apoli$modifyGunDamage(float damage, LivingEntity player, EntityHitResult hit, Entity projectile,
                                        ItemStack gunStack, @Local(name = "isHeadshot") boolean isHeadshot) {
        return GunPowers.modifyDamage(player, hit.getEntity(), PointBlankGuns.hurtingGun((Item) (Object) this, gunStack), isHeadshot, damage);
    }

    @ModifyExpressionValue(method = "hurtEntity", at = @At(value = "FIELD",
        target = "Lcom/vicmatskiv/pointblank/Config;headshotDamageModifier:D", opcode = Opcodes.GETSTATIC))
    private double apoli$modifyHeadshot(double multiplier, LivingEntity player, EntityHitResult hit, Entity projectile,
                                        ItemStack gunStack) {
        return GunPowers.modifyHeadshot(player, hit.getEntity(), PointBlankGuns.hurtingGun((Item) (Object) this, gunStack), multiplier);
    }

    @Inject(method = "hurtEntity", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private void apoli$beginGunHit(LivingEntity player, EntityHitResult hit, Entity projectile, ItemStack gunStack,
                                   CallbackInfoReturnable<Float> cir, @Local(name = "isHeadshot") boolean isHeadshot) {
        GunHit.begin(PointBlankGuns.hurtingGun((Item) (Object) this, gunStack), isHeadshot, player, null);
    }

    @Inject(method = "hurtEntity", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", shift = At.Shift.AFTER))
    private void apoli$endGunHit(LivingEntity player, EntityHitResult hit, Entity projectile, ItemStack gunStack,
                                 CallbackInfoReturnable<Float> cir) {
        GunHit.end();
    }

    @Inject(method = "handleBlockHit", at = @At("HEAD"))
    private void apoli$gunBlockHit(LivingEntity player, BlockHitResult hit, Entity projectile, CallbackInfo ci) {
        if (player == null || player.level().isClientSide()) return;
        GunPowers.onBlockHit(player, BuiltInRegistries.ITEM.getKey((Item) (Object) this), player.level(), hit.getBlockPos());
    }
}
