package dev.overgrown.apoli.keybind;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeldKeys {
    public interface ClientLookup {
        boolean isHeld(Entity entity, String key, int grace);
    }

    public static final int MAX_GRACE = 40;

    private static final Map<UUID, Set<String>> SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> FORCED = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Long>> RELEASED = new ConcurrentHashMap<>();
    private static volatile ClientLookup clientLookup;

    private HeldKeys() {}

    public static void setServerHeld(Entity owner, Collection<String> keys) {
        if (dev.overgrown.apoli.dev.DevMode.isEnabled(owner)) reportChange(owner, keys);
        MinecraftServer server = owner.level().getServer();
        recordReleases(owner.getUUID(), keys, server == null ? Long.MIN_VALUE : server.getTickCount());
        setServerHeld(owner.getUUID(), keys);
    }

    private static void reportChange(Entity owner, Collection<String> keys) {
        Set<String> before = serverHeldRaw(owner.getUUID());
        StringBuilder delta = null;
        for (String key : before) {
            if (keys.contains(key)) continue;
            delta = appendDelta(delta, '-', key);
        }
        for (String key : keys) {
            if (before.contains(key)) continue;
            delta = appendDelta(delta, '+', key);
        }
        if (delta != null) dev.overgrown.apoli.dev.DevMode.report(owner, "held keys " + delta);
    }

    private static StringBuilder appendDelta(StringBuilder delta, char sign, String key) {
        if (delta == null) return new StringBuilder().append(sign).append(key);
        return delta.append(' ').append(sign).append(key);
    }

    private static void recordReleases(UUID player, Collection<String> keys, long tick) {
        if (tick == Long.MIN_VALUE) return;
        Set<String> before = SERVER.get(player);
        if (before == null || before.isEmpty()) return;
        Map<String, Long> released = null;
        for (String key : before) {
            if (keys != null && keys.contains(key)) continue;
            if (released == null) released = RELEASED.computeIfAbsent(player, u -> new ConcurrentHashMap<>());
            released.put(key, tick);
        }
        if (released == null) return;
        for (Iterator<Map.Entry<String, Long>> it = released.entrySet().iterator(); it.hasNext(); ) {
            if (tick - it.next().getValue() > MAX_GRACE) it.remove();
        }
    }

    public static void setServerHeld(UUID player, Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            SERVER.remove(player);
        } else {
            SERVER.put(player, Set.copyOf(keys));
        }
    }

    public static boolean serverHeld(UUID entity, String key) {
        Set<String> held = SERVER.get(entity);
        if (held != null && held.contains(key)) return true;
        return forcedHeld(entity, key);
    }

    public static boolean serverHeld(UUID entity, String key, int grace, long now) {
        if (serverHeld(entity, key)) return true;
        if (grace <= 0 || now == Long.MIN_VALUE) return false;
        Map<String, Long> released = RELEASED.get(entity);
        if (released == null) return false;
        Long at = released.get(key);
        return at != null && now - at < grace;
    }

    public static Set<String> serverHeldRaw(UUID entity) {
        Set<String> held = SERVER.get(entity);
        return held == null ? Set.of() : held;
    }

    public static Set<String> serverHeldSet(UUID entity) {
        Set<String> held = SERVER.get(entity);
        Map<String, Integer> forced = FORCED.get(entity);
        if (forced == null || forced.isEmpty()) return held == null ? Set.of() : held;
        if (held == null || held.isEmpty()) return Set.copyOf(forced.keySet());
        Set<String> merged = new HashSet<>(held);
        merged.addAll(forced.keySet());
        return merged;
    }

    public static void clearServer(UUID entity) {
        SERVER.remove(entity);
        FORCED.remove(entity);
        RELEASED.remove(entity);
    }

    public static void force(UUID entity, String key, int ticks) {
        if (ticks <= 0) {
            release(entity, key);
            return;
        }
        FORCED.computeIfAbsent(entity, u -> new ConcurrentHashMap<>()).put(key, ticks);
    }

    public static void release(UUID entity, String key) {
        Map<String, Integer> keys = FORCED.get(entity);
        if (keys == null) return;
        keys.remove(key);
        if (keys.isEmpty()) FORCED.remove(entity);
    }

    public static Set<String> forcedKeys(UUID entity) {
        if (FORCED.isEmpty()) return Set.of();
        Map<String, Integer> keys = FORCED.get(entity);
        return keys == null || keys.isEmpty() ? Set.of() : keys.keySet();
    }

    public static boolean anyForced() {
        return !FORCED.isEmpty();
    }

    public static boolean forcedHeld(UUID entity, String key) {
        Map<String, Integer> keys = FORCED.get(entity);
        return keys != null && keys.containsKey(key);
    }

    public static void tickForced() {
        if (FORCED.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, Map<String, Integer>>> outer = FORCED.entrySet().iterator(); outer.hasNext(); ) {
            Map<String, Integer> keys = outer.next().getValue();
            for (Iterator<Map.Entry<String, Integer>> inner = keys.entrySet().iterator(); inner.hasNext(); ) {
                Map.Entry<String, Integer> entry = inner.next();
                int left = entry.getValue() - 1;
                if (left <= 0) inner.remove();
                else entry.setValue(left);
            }
            if (keys.isEmpty()) outer.remove();
        }
    }

    public static void setClientLookup(ClientLookup lookup) {
        clientLookup = lookup;
    }

    public static boolean clientHeld(Entity entity, String key, int grace) {
        ClientLookup lookup = clientLookup;
        return lookup != null && lookup.isHeld(entity, key, grace);
    }
}
