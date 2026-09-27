package dev.overgrown.apoli.mixin.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Enchantment.class)
public interface EnchantmentSlotsAccessor {

    @Accessor("slots")
    EquipmentSlot[] apoli$getSlots();
}
