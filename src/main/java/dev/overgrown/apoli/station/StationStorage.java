package dev.overgrown.apoli.station;

import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class StationStorage {
    private static final int FLUSH_INTERVAL = 100;
    private static final Map<UUID, Map<ResourceLocation, StationContainer>> LIVE = new HashMap<>();

    private StationStorage() {}

    public static @Nullable StationContainer live(UUID owner, ResourceLocation powerId) {
        Map<ResourceLocation, StationContainer> byPower = LIVE.get(owner);
        return byPower == null ? null : byPower.get(powerId);
    }

    public static <T extends StationContainer> @Nullable T get(PowerContainerImpl holder, UUID owner, ResourceLocation powerId,
                                                              Class<T> type, Supplier<T> factory, boolean create) {
        StationContainer live = live(owner, powerId);
        if (type.isInstance(live)) return type.cast(live);
        if (live != null) discard(owner, powerId);
        CompoundTag stored = holder.getAuxNbt(powerId);
        if (stored == null && !create) return null;
        T made = factory.get();
        made.load(stored);
        LIVE.computeIfAbsent(owner, k -> new HashMap<>(2)).put(powerId, made);
        return made;
    }

    public static @Nullable StationContainer discard(UUID owner, ResourceLocation powerId) {
        Map<ResourceLocation, StationContainer> byPower = LIVE.get(owner);
        if (byPower == null) return null;
        StationContainer removed = byPower.remove(powerId);
        if (byPower.isEmpty()) LIVE.remove(owner);
        if (removed != null) removed.markRemoved();
        return removed;
    }

    public static void flush(StationContainer station, PowerContainerImpl holder) {
        if (!holder.allPowers().contains(station.powerId())) return;
        holder.setAuxNbt(station.powerId(), station.save());
    }

    public static void flush(StationContainer station, Entity owner) {
        if (PowerContainer.of(owner) instanceof PowerContainerImpl impl) flush(station, impl);
    }

    public static void tickFlush(StationContainer station, PowerContainerImpl holder) {
        if (station.shouldFlush(FLUSH_INTERVAL)) flush(station, holder);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        Map<ResourceLocation, StationContainer> byPower = LIVE.remove(player.getUUID());
        if (byPower == null) return;
        for (StationContainer station : byPower.values()) {
            flush(station, player);
            station.markRemoved();
        }
    }

    public static void flushAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Map<ResourceLocation, StationContainer> byPower = LIVE.get(player.getUUID());
            if (byPower == null) continue;
            for (StationContainer station : byPower.values()) flush(station, player);
        }
    }

    public static void clear() {
        LIVE.clear();
    }
}
