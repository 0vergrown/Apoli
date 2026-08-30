package dev.overgrown.apoli.keybind;

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
        boolean isHeld(Entity entity, String key);
    }

    private static final Map<UUID, Set<String>> SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> FORCED = new ConcurrentHashMap<>();
    private static volatile ClientLookup clientLookup;

    private HeldKeys() {}

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

    public static boolean clientHeld(Entity entity, String key) {
        ClientLookup lookup = clientLookup;
        return lookup != null && lookup.isHeld(entity, key);
    }
}
