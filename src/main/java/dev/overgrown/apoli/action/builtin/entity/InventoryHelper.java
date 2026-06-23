package dev.overgrown.apoli.action.builtin.entity;

import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.ItemSlot;
import dev.overgrown.apoli.data.ProcessMode;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class InventoryHelper {
    private InventoryHelper() {}

    public static int walk(
        Container container,
        LivingEntity entity,
        Optional<ItemCondition> itemCondition,
        Optional<ItemSlot> slot,
        Optional<List<ItemSlot>> slots,
        ProcessMode mode,
        int limit,
        BiConsumer<Integer, ItemStack> visitor
    ) {
        int visited = 0;
        int max = container.getContainerSize();
        for (int idx = 0; idx < max; idx++) {
            if (limit > 0 && visited >= limit) break;
            if (!slotMatches(idx, entity, slot, slots)) continue;
            ItemStack stack = container.getItem(idx);
            if (stack.isEmpty()) continue;
            if (itemCondition.isPresent()
                && !itemCondition.get().test(new ItemCtx(stack, entity.level(), entity))) continue;
            if (mode == ProcessMode.ITEMS) {
                int count = stack.getCount();
                for (int j = 0; j < count; j++) {
                    visitor.accept(idx, stack);
                    visited++;
                    if (limit > 0 && visited >= limit) break;
                }
            } else {
                visitor.accept(idx, stack);
                visited++;
            }
        }
        return visited;
    }

    private static boolean slotMatches(int containerIndex, LivingEntity entity,
                                       Optional<ItemSlot> slot, Optional<List<ItemSlot>> slots) {
        if (slot.isEmpty() && slots.isEmpty()) return true;
        if (slot.isPresent() && resolveIndex(slot.get(), entity) == containerIndex) return true;
        if (slots.isPresent()) {
            for (ItemSlot s : slots.get()) if (resolveIndex(s, entity) == containerIndex) return true;
        }
        return false;
    }

    private static int resolveIndex(ItemSlot slot, LivingEntity entity) {
        if (slot.index() == ItemSlot.MAINHAND && entity instanceof Player player) {
            return player.getInventory().selected;
        }
        return slot.toContainerIndex();
    }
}
