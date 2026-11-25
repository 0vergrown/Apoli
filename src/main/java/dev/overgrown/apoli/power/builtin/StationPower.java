package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.station.StationContainer;
import dev.overgrown.apoli.station.StationSettings;
import dev.overgrown.apoli.station.StationStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class StationPower<C extends StationPower.Config> extends PowerType<C> {
    public interface Config {
        StationSettings settings();
    }

    protected abstract StationContainer create(UUID owner, ResourceLocation powerId, C cfg);

    protected abstract boolean matches(StationContainer station, C cfg);

    protected @Nullable StationContainer station(ResourceLocation powerId, C cfg, ServerPlayer player,
                                                 PowerContainerImpl holder, boolean create) {
        UUID owner = player.getUUID();
        StationContainer live = StationStorage.live(owner, powerId);
        if (live != null) {
            if (matches(live, cfg)) return live;
            StationStorage.flush(live, holder);
            StationStorage.discard(owner, powerId);
        }
        CompoundTag stored = holder.getAuxNbt(powerId);
        if (stored == null && !create) return null;
        StationContainer made = create(owner, powerId, cfg);
        StationStorage.adopt(made, stored, holder);
        return made;
    }

    protected abstract boolean tickStation(StationContainer station, C cfg, ServerPlayer player);

    protected abstract MenuProvider menuProvider(StationContainer station, C cfg, Component title);

    protected abstract Component defaultTitle(C cfg);

    public void open(ResourceLocation powerId, C cfg, ServerPlayer player, PowerContainerImpl holder) {
        StationContainer station = station(powerId, cfg, player, holder, true);
        if (station == null) return;
        Component title = cfg.settings().title().orElseGet(() -> defaultTitle(cfg));
        player.openMenu(menuProvider(station, cfg, title));
    }

    @Override
    public void tick(ResourceLocation powerId, C cfg, PowerContainer holder) {
        if (!(holder.owner() instanceof ServerPlayer player) || !(holder instanceof PowerContainerImpl impl)) return;
        StationContainer station = station(powerId, cfg, player, impl, false);
        if (station == null) return;
        StationSettings settings = cfg.settings();
        if ((settings.tickWhileClosed() || station.viewed()) && !station.idle(settings)
            && tickStation(station, cfg, player) && settings.finishAction().isPresent()) {
            settings.finishAction().get().run(new EntityCtx(player, player.serverLevel()));
        }
        StationStorage.tickFlush(station, impl);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, C cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.allPowers().contains(powerId)) return;
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (holder.owner() instanceof ServerPlayer player) {
            StationContainer station = StationStorage.live(player.getUUID(), powerId);
            if (station == null) station = station(powerId, cfg, player, impl, false);
            if (station != null) {
                StationStorage.discard(player.getUUID(), powerId);
                if (cfg.settings().recoverable()) {
                    for (int slot = 0; slot < station.getContainerSize(); slot++) {
                        ItemStack stack = station.getItem(slot);
                        if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack);
                    }
                }
                station.clearContent();
            }
        }
        impl.removeAux(powerId);
    }

    public void dropOnDeath(ResourceLocation powerId, C cfg, LivingEntity dead, PowerContainerImpl holder, ServerLevel level) {
        if (!(dead instanceof ServerPlayer player)) return;
        StationContainer station = station(powerId, cfg, player, holder, false);
        if (station == null) return;
        for (int slot = 0; slot < station.getContainerSize(); slot++) {
            ItemStack stack = station.getItem(slot);
            if (stack.isEmpty()) continue;
            if (cfg.settings().dropOnDeathFilter().isPresent()
                && !cfg.settings().dropOnDeathFilter().get().test(new ItemCtx(stack, level, dead))) continue;
            dead.spawnAtLocation(stack);
            station.setItem(slot, ItemStack.EMPTY);
        }
        StationStorage.flush(station, holder);
    }

    public static boolean open(ResourceLocation powerId, ServerPlayer player) {
        if (!(PowerContainer.of(player) instanceof PowerContainerImpl impl)) return false;
        if (!impl.hasPower(powerId) || impl.isSuppressed(powerId)) return false;
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || !(PowerTypeRegistry.get(loaded.typeId()) instanceof StationPower<?> type)) return false;
        type.openUnchecked(powerId, loaded.config(), player, impl);
        return true;
    }

    public static @Nullable StationContainer find(LivingEntity holder, ResourceLocation powerId) {
        if (!(holder instanceof ServerPlayer player)) return null;
        if (!(PowerContainer.of(player) instanceof PowerContainerImpl impl) || !impl.hasPower(powerId)) return null;
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || !(PowerTypeRegistry.get(loaded.typeId()) instanceof StationPower<?> type)) return null;
        return type.stationUnchecked(powerId, loaded.config(), player, impl);
    }

    @SuppressWarnings("unchecked")
    private void openUnchecked(ResourceLocation powerId, Object cfg, ServerPlayer player, PowerContainerImpl holder) {
        open(powerId, (C) cfg, player, holder);
    }

    @SuppressWarnings("unchecked")
    private @Nullable StationContainer stationUnchecked(ResourceLocation powerId, Object cfg, ServerPlayer player,
                                                        PowerContainerImpl holder) {
        return station(powerId, (C) cfg, player, holder, false);
    }

    @SuppressWarnings("unchecked")
    public void dropOnDeathUnchecked(ResourceLocation powerId, Object cfg, LivingEntity dead, PowerContainerImpl holder,
                                     ServerLevel level) {
        dropOnDeath(powerId, (C) cfg, dead, holder, level);
    }
}
