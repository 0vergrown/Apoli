package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.attribution.PowerCause;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ItemPickupHandler {
    private ItemPickupHandler() {}

    public record Match<C>(ResourceLocation powerId, C config, int priority) {}

    public record Pending(ItemEntity item, @Nullable Entity thrower,
                          List<Match<ActionOnItemPickupPower.Config>> actions, int count) {
        public boolean taken() {
            return item.isRemoved() || item.getItem().getCount() < count;
        }
    }

    private static final Comparator<Match<?>> BY_PRIORITY =
        Comparator.comparingInt((Match<?> match) -> match.priority()).reversed();

    public static boolean watches(@Nullable Entity holder) {
        if (holder == null) return false;
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || container.isEmpty()) return false;
        return !container.powersOfType(ApoliIds.PREVENT_ITEM_PICKUP).isEmpty()
            || !container.powersOfType(ApoliIds.ACTION_ON_ITEM_PICKUP).isEmpty();
    }

    public static boolean hasRoom(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) return true;
        Inventory inventory = player.getInventory();
        return inventory.getSlotWithRemainingSpace(stack) >= 0 || inventory.getFreeSlot() >= 0;
    }

    public static boolean prevents(ItemEntity item, Entity holder) {
        Level level = holder.level();
        LivingEntity living = holder instanceof LivingEntity le ? le : null;
        Entity thrower = item.getOwner();
        ItemStack stack = item.getItem();
        List<Match<PreventItemPickupPower.Config>> matches = new ArrayList<>(2);
        PowerLookup.forEachEntry(holder, ApoliIds.PREVENT_ITEM_PICKUP, PreventItemPickupPower.Config.class, (id, cfg) -> {
            if (applies(cfg.itemCondition(), cfg.bientityCondition(), stack, level, living, thrower, holder)) {
                matches.add(new Match<>(id, cfg, cfg.priority()));
            }
        });
        if (matches.isEmpty()) return false;
        if (matches.size() > 1) matches.sort(BY_PRIORITY);
        for (int i = 0; i < matches.size(); i++) {
            Match<PreventItemPickupPower.Config> match = matches.get(i);
            PreventItemPickupPower.Config cfg = match.config();
            boolean attributed = PowerCause.push(holder, match.powerId());
            try {
                if (cfg.itemAction().isPresent()) {
                    cfg.itemAction().get().run(new ItemCtx(item.getItem(), level, living, item::setItem));
                }
                if (cfg.bientityActionThrower().isPresent()) {
                    cfg.bientityActionThrower().get().run(BiEntityCtx.of(thrower, holder, level));
                }
                if (cfg.bientityActionItem().isPresent()) {
                    cfg.bientityActionItem().get().run(BiEntityCtx.of(holder, item, level));
                }
            } finally {
                if (attributed) PowerCause.pop();
            }
        }
        return true;
    }

    public static @Nullable Pending before(ItemEntity item, Entity holder, boolean runItemActions) {
        Level level = holder.level();
        LivingEntity living = holder instanceof LivingEntity le ? le : null;
        Entity thrower = item.getOwner();
        ItemStack stack = item.getItem();
        List<Match<ActionOnItemPickupPower.Config>> matches = new ArrayList<>(2);
        PowerLookup.forEachEntry(holder, ApoliIds.ACTION_ON_ITEM_PICKUP, ActionOnItemPickupPower.Config.class, (id, cfg) -> {
            if (applies(cfg.itemCondition(), cfg.bientityCondition(), stack, level, living, thrower, holder)) {
                matches.add(new Match<>(id, cfg, cfg.priority()));
            }
        });
        if (matches.isEmpty()) return null;
        if (matches.size() > 1) matches.sort(BY_PRIORITY);
        if (runItemActions) {
            for (int i = 0; i < matches.size(); i++) {
                Match<ActionOnItemPickupPower.Config> match = matches.get(i);
                if (match.config().itemAction().isEmpty()) continue;
                boolean attributed = PowerCause.push(holder, match.powerId());
                try {
                    match.config().itemAction().get().run(new ItemCtx(item.getItem(), level, living, item::setItem));
                } finally {
                    if (attributed) PowerCause.pop();
                }
            }
        }
        return new Pending(item, thrower, matches, item.getItem().getCount());
    }

    public static void after(Pending pending, Entity holder) {
        Level level = holder.level();
        List<Match<ActionOnItemPickupPower.Config>> matches = pending.actions();
        for (int i = 0; i < matches.size(); i++) {
            Match<ActionOnItemPickupPower.Config> match = matches.get(i);
            if (match.config().bientityAction().isEmpty()) continue;
            boolean attributed = PowerCause.push(holder, match.powerId());
            try {
                match.config().bientityAction().get().run(BiEntityCtx.of(pending.thrower(), holder, level));
            } finally {
                if (attributed) PowerCause.pop();
            }
        }
    }

    private static boolean applies(Optional<ItemCondition> itemCondition, Optional<BiEntityCondition> bientityCondition,
                                   ItemStack stack, Level level, @Nullable LivingEntity living,
                                   @Nullable Entity thrower, Entity holder) {
        if (itemCondition.isPresent() && !itemCondition.get().test(new ItemCtx(stack, level, living))) return false;
        return bientityCondition.isEmpty() || bientityCondition.get().test(BiEntityCtx.of(thrower, holder, level));
    }
}
