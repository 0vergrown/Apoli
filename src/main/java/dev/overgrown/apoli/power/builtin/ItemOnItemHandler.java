package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ItemOnItemHandler {

    private ItemOnItemHandler() {}

    public static boolean handle(Player player, Slot slot, SlotAccess cursor, ClickAction clickAction) {
        if (player == null) return false;
        PowerContainer container = PowerContainer.of(player);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.ITEM_ON_ITEM);
        if (powers.isEmpty()) return false;

        ItemOnItemPower.ClickType clickType = clickAction == ClickAction.PRIMARY
            ? ItemOnItemPower.ClickType.PRIMARY
            : ItemOnItemPower.ClickType.SECONDARY;

        Level level = player.level();
        boolean server = !level.isClientSide();
        EntityCtx entityCtx = null;
        boolean applied = false;

        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof ItemOnItemPower.Config cfg)) continue;
            if (cfg.clickType() != clickType) continue;
            if (cfg.usingItemCondition().isPresent()
                && !cfg.usingItemCondition().get().test(new ItemCtx(cursor.get(), level, player))) continue;
            if (cfg.onItemCondition().isPresent()
                && !cfg.onItemCondition().get().test(new ItemCtx(slot.getItem(), level, player))) continue;
            if (power.condition().isPresent()) {
                if (entityCtx == null) entityCtx = EntityCtx.of(player, level);
                if (!power.condition().get().test(entityCtx)) continue;
            }
            applied = true;
            if (server) execute(player, level, slot, cursor, cfg);
        }

        return applied;
    }

    private static void execute(Player player, Level level, Slot slot, SlotAccess cursor, ItemOnItemPower.Config cfg) {
        ItemStack result;
        if (cfg.result().isPresent()) {
            result = cfg.result().get().stack().copy();
        } else if (cfg.resultFromOnStack() > 0) {
            result = slot.getItem().split(cfg.resultFromOnStack());
        } else {
            result = slot.getItem();
        }

        if (cfg.resultItemAction().isPresent()) {
            ItemStack[] held = {result};
            cfg.resultItemAction().get().run(new ItemCtx(result, level, player, s -> held[0] = s));
            result = held[0];
        }

        runOnCursor(player, level, cursor, cfg.usingItemAction());
        runOnSlot(player, level, slot, cfg.onItemAction());

        if (cfg.result().isPresent() || cfg.resultItemAction().isPresent()) {
            if (slot.hasItem()) {
                if (!player.getInventory().add(result)) player.drop(result, false);
            } else {
                slot.set(result);
            }
        }

        if (cfg.entityAction().isPresent()) {
            cfg.entityAction().get().run(EntityCtx.of(player, level));
        }
    }

    private static void runOnCursor(Player player, Level level, SlotAccess cursor, java.util.Optional<ItemAction> action) {
        if (action.isEmpty()) return;
        ItemStack stack = cursor.get();
        if (stack.isEmpty()) return;
        action.get().run(new ItemCtx(stack, level, player, cursor::set));
        if (cursor.get().isEmpty()) cursor.set(ItemStack.EMPTY);
    }

    private static void runOnSlot(Player player, Level level, Slot slot, java.util.Optional<ItemAction> action) {
        if (action.isEmpty()) return;
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;
        action.get().run(new ItemCtx(stack, level, player, slot::set));
        if (slot.getItem().isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
    }
}
