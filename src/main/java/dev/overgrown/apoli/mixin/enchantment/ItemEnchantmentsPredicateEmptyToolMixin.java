package dev.overgrown.apoli.mixin.enchantment;

import dev.overgrown.apoli.power.builtin.ModifyEnchantmentLevelHandler;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemEnchantmentsPredicate.Enchantments.class)
public abstract class ItemEnchantmentsPredicateEmptyToolMixin implements SingleComponentItemPredicate<ItemEnchantments> {

    @Override
    public boolean matches(ItemStack stack) {
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) {
            if (!ModifyEnchantmentLevelHandler.inToolScope(stack)) return false;
            enchantments = ItemEnchantments.EMPTY;
        }
        return this.matches(stack, enchantments);
    }
}
