package dev.overgrown.apoli.mixin.enchantment;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.ModifyEnchantmentLevelHandler;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperModifyLevelMixin {

    @ModifyReturnValue(method = "getItemEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/item/ItemStack;)I",
        at = @At("RETURN"))
    private static int apoli$modifyItemLevel(int original, Enchantment enchantment, ItemStack stack) {
        return ModifyEnchantmentLevelHandler.level(stack, enchantment, original);
    }

    @ModifyReturnValue(method = "getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I",
        at = @At("RETURN"))
    private static int apoli$modifyEmptySlotLevel(int original, Enchantment enchantment, LivingEntity living) {
        return ModifyEnchantmentLevelHandler.emptySlotLevel(living, enchantment, original);
    }

    @ModifyExpressionValue(method = "runIterationOnItem(Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getEnchantmentTags()Lnet/minecraft/nbt/ListTag;"))
    private static ListTag apoli$modifyIteratedEnchantments(ListTag original, @Local(argsOnly = true) ItemStack stack) {
        return ModifyEnchantmentLevelHandler.enchantmentTags(stack, original);
    }
}
