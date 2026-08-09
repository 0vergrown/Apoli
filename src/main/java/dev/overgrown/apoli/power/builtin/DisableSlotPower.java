package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.ItemSlot;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DisableSlotPower extends PowerType<DisableSlotPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("disable_slot");

    public static final int INVENTORY_SIZE = 41;

    public record Config(List<ItemSlot> slots, Optional<ItemCondition> itemCondition, boolean mainHand) {
        public boolean blocks(ItemStack stack, Player holder) {
            if (itemCondition.isEmpty()) return true;
            return itemCondition.get().test(new ItemCtx(stack, holder.level(), holder));
        }

        public boolean covers(int containerIndex, int selected) {
            if (mainHand && containerIndex == selected) return true;
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i).toContainerIndex() == containerIndex) return true;
            }
            return false;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ItemSlot.CODEC.optionalFieldOf("slot").forGetter(c -> Optional.empty()),
            ItemSlot.CODEC.listOf().optionalFieldOf("slots").forGetter(c -> Optional.of(c.slots())),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition)
        ).apply(i, DisableSlotPower::build));
    }

    private static Config build(Optional<ItemSlot> slot, Optional<List<ItemSlot>> slots,
                                Optional<ItemCondition> itemCondition) {
        Set<ItemSlot> merged = new LinkedHashSet<>();
        slot.ifPresent(merged::add);
        slots.ifPresent(merged::addAll);

        Set<ItemSlot> supported = new LinkedHashSet<>();
        boolean mainHand = false;
        for (ItemSlot s : merged) {
            if (s.index() == ItemSlot.MAINHAND) {
                mainHand = true;
                continue;
            }
            int index = s.toContainerIndex();
            if (index < 0 || index >= INVENTORY_SIZE) {
                Apoli.LOGGER.warn("[Apoli] apoli:disable_slot ignores '{}' — it only covers the player's own "
                    + "inventory (hotbar.*, inventory.*, armor.*, weapon.*).", s.name());
                continue;
            }
            supported.add(s);
        }
        return new Config(List.copyOf(supported), itemCondition, mainHand);
    }

    @Override
    public void onAdded(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        vacate(holder.owner(), cfg);
    }

    @Override
    public void onUnsuppressed(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        vacate(holder.owner(), cfg);
    }

    private static void vacate(@Nullable LivingEntity holder, Config cfg) {
        if (!(holder instanceof Player player) || player.level().isClientSide()) return;
        Inventory inventory = player.getInventory();
        int selected = inventory.selected;

        if (cfg.mainHand()) relocate(player, inventory, cfg, selected);
        List<ItemSlot> slots = cfg.slots();
        for (int i = 0; i < slots.size(); i++) {
            relocate(player, inventory, cfg, slots.get(i).toContainerIndex());
        }
    }

    private static void relocate(Player player, Inventory inventory, Config cfg, int index) {
        if (index < 0 || index >= INVENTORY_SIZE) return;
        ItemStack stack = inventory.getItem(index);
        if (stack.isEmpty() || !cfg.blocks(stack, player)) return;
        inventory.setItem(index, ItemStack.EMPTY);
        if (!giveBack(player, stack) && !stack.isEmpty()) {
            player.drop(stack, false, true);
        }
    }

    public static boolean has(@Nullable Entity entity) {
        return PowerLookup.hasActive(entity, CANONICAL);
    }

    public static boolean disabled(Player player, int containerIndex, ItemStack stack) {
        int selected = player.getInventory().selected;
        boolean[] hit = new boolean[]{false};
        PowerLookup.forEach(player, CANONICAL, Config.class, cfg -> {
            if (hit[0]) return;
            if (!cfg.covers(containerIndex, selected)) return;
            if (stack.isEmpty() || cfg.blocks(stack, player)) hit[0] = true;
        });
        return hit[0];
    }

    public static boolean giveBack(Player player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        for (int index = 0; index < Inventory.INVENTORY_SIZE && !stack.isEmpty(); index++) {
            ItemStack existing = inventory.getItem(index);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) continue;
            if (disabled(player, index, stack)) continue;
            int room = Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize()) - existing.getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, stack.getCount());
            existing.grow(moved);
            stack.shrink(moved);
        }
        for (int index = 0; index < Inventory.INVENTORY_SIZE && !stack.isEmpty(); index++) {
            if (!inventory.getItem(index).isEmpty()) continue;
            if (disabled(player, index, stack)) continue;
            inventory.setItem(index, stack.copyAndClear());
        }
        return stack.isEmpty();
    }

}
