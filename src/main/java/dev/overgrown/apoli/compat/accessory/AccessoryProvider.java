package dev.overgrown.apoli.compat.accessory;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public interface AccessoryProvider {

    String id();

    boolean isPresent();

    List<AccessorySlotRef> equipped(LivingEntity entity);

    List<AccessorySlotRef> slots(LivingEntity entity);

    ItemStack equip(LivingEntity entity, List<AccessorySlot> filter, ItemStack stack);

    default boolean isAccessory(ItemStack stack, Level level) {
        return false;
    }

    default void applySlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {}

    default void removeSlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {}
}
