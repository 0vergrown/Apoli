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

public final class StationStorage {
    private static final int FLUSH_INTERVAL = 100;
    private static final Map<UUID, Map<ResourceLocation, StationContainer>> LIVE = new HashMap<>();

    private StationStorage() {}

    public static @Nullable StationContainer live(UUID owner, ResourceLocation powerId) {
        Map<ResourceLocation, StationContainer> byPower = LIVE.get(owner);
        return byPower == null ? null : byPower.get(powerId);
    }

    public static void adopt(StationContainer station, @Nullable CompoundTag stored, PowerContainerImpl holder) {
        station.load(stored, holder.rawOwner().registryAccess());
        LIVE.computeIfAbsent(station.owner(), k -> new HashMap<>(2)).put(station.powerId(), station);
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
        holder.storeAuxNbt(station.powerId(), station.save(holder.rawOwner().registryAccess()));
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
