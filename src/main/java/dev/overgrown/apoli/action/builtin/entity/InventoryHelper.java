package dev.overgrown.apoli.action.builtin.entity;

import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.SimpleContainer;
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
        return walk(container, entity, itemCondition, slot, slots, mode, limit, false, visitor);
    }

    public static int walk(
        Container container,
        LivingEntity entity,
        Optional<ItemCondition> itemCondition,
        Optional<ItemSlot> slot,
        Optional<List<ItemSlot>> slots,
        ProcessMode mode,
        int limit,
        boolean includeEmpty,
        BiConsumer<Integer, ItemStack> visitor
    ) {
        int visited = 0;
        int max = container.getContainerSize();
        for (int idx = 0; idx < max; idx++) {
            if (limit > 0 && visited >= limit) break;
            if (!slotMatches(idx, entity, slot, slots)) continue;
            ItemStack stack = container.getItem(idx);
            if (stack.isEmpty() && !includeEmpty) continue;
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

    private static final EquipmentSlot[] EQUIPMENT = EquipmentSlot.values();

    public interface SlotVisitor {
        void accept(SlotAccess slot, ItemStack stack);
    }

    public static int walkEntity(
        LivingEntity entity,
        Optional<ItemCondition> itemCondition,
        Optional<ItemSlot> slot,
        Optional<List<ItemSlot>> slots,
        ProcessMode mode,
        int limit,
        boolean includeEmpty,
        SlotVisitor visitor
    ) {
        int visited = 0;
        if (slot.isPresent() || slots.isPresent()) {
            if (slot.isPresent()) {
                visited = visitSlot(entity, entity.getSlot(slot.get().index()), itemCondition, mode, limit,
                    includeEmpty, visited, visitor);
            }
            if (slots.isPresent()) {
                List<ItemSlot> list = slots.get();
                for (int i = 0; i < list.size(); i++) {
                    int index = list.get(i).index();
                    if (listedEarlier(index, slot, list, i)) continue;
                    visited = visitSlot(entity, entity.getSlot(index), itemCondition, mode, limit, includeEmpty,
                        visited, visitor);
                }
            }
            return visited;
        }
        for (EquipmentSlot equipment : EQUIPMENT) {
            visited = visitSlot(entity, SlotAccess.forEquipmentSlot(entity, equipment), itemCondition, mode, limit,
                includeEmpty, visited, visitor);
        }
        if (entity instanceof InventoryCarrier carrier) {
            SimpleContainer inventory = carrier.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                visited = visitSlot(entity, SlotAccess.forContainer(inventory, i), itemCondition, mode, limit,
                    includeEmpty, visited, visitor);
            }
        }
        return visited;
    }

    private static boolean listedEarlier(int index, Optional<ItemSlot> slot, List<ItemSlot> list, int position) {
        if (slot.isPresent() && slot.get().index() == index) return true;
        for (int j = 0; j < position; j++) {
            if (list.get(j).index() == index) return true;
        }
        return false;
    }

    private static int visitSlot(LivingEntity entity, SlotAccess access, Optional<ItemCondition> itemCondition,
                                 ProcessMode mode, int limit, boolean includeEmpty, int visited,
                                 SlotVisitor visitor) {
        if (limit > 0 && visited >= limit) return visited;
        if (access == SlotAccess.NULL) return visited;
        ItemStack stack = access.get();
        if (stack.isEmpty() && !includeEmpty) return visited;
        if (itemCondition.isPresent()
            && !itemCondition.get().test(new ItemCtx(stack, entity.level(), entity))) return visited;
        if (mode == ProcessMode.ITEMS) {
            int count = stack.getCount();
            for (int j = 0; j < count; j++) {
                visitor.accept(access, stack);
                visited++;
                if (limit > 0 && visited >= limit) break;
            }
        } else {
            visitor.accept(access, stack);
            visited++;
        }
        return visited;
    }
}
