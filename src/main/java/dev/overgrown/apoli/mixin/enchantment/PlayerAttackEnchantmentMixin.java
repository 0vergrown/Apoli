package dev.overgrown.apoli.mixin.enchantment;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.power.builtin.ModifyEnchantmentLevelHandler;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerAttackEnchantmentMixin {

    @WrapOperation(method = "attack", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getDamageBonus(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/MobType;)F"))
    private float apoli$emptyHandDamageBonus(ItemStack stack, MobType mobType, Operation<Float> original) {
        float bonus = original.call(stack, mobType);
        return stack.isEmpty() ? bonus + ModifyEnchantmentLevelHandler.emptyDamageBonus((Player) (Object) this, mobType) : bonus;
    }
}
