package dev.overgrown.apoli.mixin.enchantment;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.ModifyEnchantmentLevelHandler;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemPredicate.class)
public abstract class ItemPredicateModifyLevelMixin {

    @ModifyExpressionValue(method = "matches(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getEnchantmentTags()Lnet/minecraft/nbt/ListTag;"))
    private ListTag apoli$modifyMatchedEnchantments(ListTag original, @Local(argsOnly = true) ItemStack stack) {
        return ModifyEnchantmentLevelHandler.enchantmentTags(stack, original);
    }
}
