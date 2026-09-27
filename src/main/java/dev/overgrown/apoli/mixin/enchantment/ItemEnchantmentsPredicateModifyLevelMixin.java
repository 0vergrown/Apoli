package dev.overgrown.apoli.mixin.enchantment;

import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.ModifyEnchantmentLevelHandler;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemEnchantmentsPredicate.class)
public abstract class ItemEnchantmentsPredicateModifyLevelMixin {

    @ModifyVariable(method = "matches(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/ItemEnchantments;)Z",
        at = @At("HEAD"), argsOnly = true)
    private ItemEnchantments apoli$modifyMatchedEnchantments(ItemEnchantments enchantments, @Local(argsOnly = true) ItemStack stack) {
        if (!((Object) this instanceof ItemEnchantmentsPredicate.Enchantments)) return enchantments;
        return ModifyEnchantmentLevelHandler.enchantments(stack, enchantments);
    }
}
