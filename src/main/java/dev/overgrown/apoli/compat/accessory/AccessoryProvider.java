package dev.overgrown.apoli.compat.accessory;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public interface AccessoryProvider {

    String id();

    boolean isPresent();

    List<AccessorySlotRef> equipped(LivingEntity entity);

    List<AccessorySlotRef> slots(LivingEntity entity);

    ItemStack equip(LivingEntity entity, List<AccessorySlot> filter, ItemStack stack);

    default int count(LivingEntity entity, List<AccessorySlot> filter, @Nullable Predicate<ItemStack> item, int limit) {
        int count = 0;
        for (AccessorySlotRef ref : equipped(entity)) {
            if (!AccessorySlot.matchesAny(filter, ref)) continue;
            if (item != null && !item.test(ref.getStack())) continue;
            if (++count >= limit) break;
        }
        return count;
    }

    default boolean isAccessory(ItemStack stack, Level level) {
        return false;
    }

    default void applySlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {}

    default void removeSlotModifiers(LivingEntity entity, Multimap<String, AttributeModifier> modifiers) {}
}
