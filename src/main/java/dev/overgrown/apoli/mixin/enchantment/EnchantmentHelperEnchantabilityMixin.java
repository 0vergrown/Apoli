package dev.overgrown.apoli.mixin.enchantment;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.ModifyEnchantabilityPower;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperEnchantabilityMixin {

    @ModifyExpressionValue(method = "selectEnchantment", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/Item;getEnchantmentValue()I"))
    private static int apoli$modifyEnchantability(int original, RandomSource random, ItemStack stack, int level,
                                                  boolean treasure) {
        return ModifyEnchantabilityPower.modify(stack, original);
    }
}
