package dev.overgrown.apoli.data;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class SlotStacks {
    private SlotStacks() {}

    public static final int SCOPE_SLOT = 0;
    public static final int SCOPE_EQUIPMENT = 1;
    public static final int SCOPE_ARMOR = 2;
    public static final int SCOPE_HANDS = 3;

    private static final EquipmentSlot[] EQUIPMENT = EquipmentSlot.values();
    private static final EquipmentSlot[] ARMOR = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final EquipmentSlot[] HANDS = { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND };

    public static int scopeOf(String name) {
        return switch (name) {
            case "any", "all", "equipment" -> SCOPE_EQUIPMENT;
            case "armor", "armour" -> SCOPE_ARMOR;
            case "hands", "held" -> SCOPE_HANDS;
            default -> -1;
        };
    }

    public static EquipmentSlot[] slotsOf(int scope) {
        return switch (scope) {
            case SCOPE_ARMOR -> ARMOR;
            case SCOPE_HANDS -> HANDS;
            default -> EQUIPMENT;
        };
    }

    public static ItemStack get(@Nullable Entity entity, ItemSlot slot) {
        if (entity == null) return ItemStack.EMPTY;
        EquipmentSlot equipment = equipmentOf(slot);
        if (equipment != null) {
            return entity instanceof LivingEntity living ? living.getItemBySlot(equipment) : ItemStack.EMPTY;
        }
        if (!(entity instanceof Player player)) return ItemStack.EMPTY;
        if (slot.isEnderChest()) {
            int index = slot.enderChestIndex();
            return index < player.getEnderChestInventory().getContainerSize()
                ? player.getEnderChestInventory().getItem(index)
                : ItemStack.EMPTY;
        }
        int index = slot.toContainerIndex();
        if (index < 0 || index >= player.getInventory().getContainerSize()) return ItemStack.EMPTY;
        return player.getInventory().getItem(index);
    }

    @Nullable
    public static EquipmentSlot equipmentOf(ItemSlot slot) {
        return switch (slot.index()) {
            case ItemSlot.MAINHAND -> EquipmentSlot.MAINHAND;
            case ItemSlot.OFFHAND -> EquipmentSlot.OFFHAND;
            case ItemSlot.HEAD -> EquipmentSlot.HEAD;
            case ItemSlot.CHEST -> EquipmentSlot.CHEST;
            case ItemSlot.LEGS -> EquipmentSlot.LEGS;
            case ItemSlot.FEET -> EquipmentSlot.FEET;
            default -> null;
        };
    }
}
