package dev.overgrown.apoli.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class OfflinePlayers {
    private static final int CAPACITY = 32;

    private static final Map<UUID, OfflinePlayerSnapshot> SNAPSHOTS = new LinkedHashMap<>(16, 0.75f, true);
    private static final Set<UUID> WITHOUT_DATA = new HashSet<>();

    private OfflinePlayers() {}

    public static @Nullable ServerPlayer get(MinecraftServer server, UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) return online;
        OfflinePlayerSnapshot cached = SNAPSHOTS.get(uuid);
        if (cached != null) return cached;
        if (WITHOUT_DATA.contains(uuid)) return null;
        OfflinePlayerSnapshot loaded = hasSavedData(server, uuid) ? OfflinePlayerSnapshot.load(server, profile(server, uuid)) : null;
        if (loaded == null) {
            WITHOUT_DATA.add(uuid);
            return null;
        }
        SNAPSHOTS.put(uuid, loaded);
        if (SNAPSHOTS.size() > CAPACITY) {
            Iterator<UUID> eldest = SNAPSHOTS.keySet().iterator();
            eldest.next();
            eldest.remove();
        }
        return loaded;
    }

    public static boolean isSnapshot(Entity entity) {
        return entity instanceof OfflinePlayerSnapshot;
    }

    public static void forget(UUID uuid) {
        SNAPSHOTS.remove(uuid);
        WITHOUT_DATA.remove(uuid);
    }

    public static void clear() {
        SNAPSHOTS.clear();
        WITHOUT_DATA.clear();
    }

    private static boolean hasSavedData(MinecraftServer server, UUID uuid) {
        return Files.isRegularFile(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid + ".dat"));
    }

    private static GameProfile profile(MinecraftServer server, UUID uuid) {
        GameProfileCache cache = server.getProfileCache();
        if (cache != null) {
            Optional<GameProfile> known = cache.get(uuid);
            if (known.isPresent()) return known.get();
        }
        return new GameProfile(uuid, uuid.toString().replace("-", "").substring(0, 16));
    }
}
