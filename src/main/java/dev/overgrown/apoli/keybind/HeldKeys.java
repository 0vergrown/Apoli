package dev.overgrown.apoli.keybind;

import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeldKeys {
    public interface ClientLookup {
        boolean isHeld(Entity entity, String key);
    }

    private static final Map<UUID, Set<String>> SERVER = new ConcurrentHashMap<>();
    private static volatile ClientLookup clientLookup;

    private HeldKeys() {}

    public static void setServerHeld(UUID player, Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            SERVER.remove(player);
        } else {
            SERVER.put(player, Set.copyOf(keys));
        }
    }

    public static boolean serverHeld(UUID player, String key) {
        Set<String> held = SERVER.get(player);
        return held != null && held.contains(key);
    }

    public static Set<String> serverHeldSet(UUID player) {
        Set<String> held = SERVER.get(player);
        return held == null ? Set.of() : held;
    }

    public static void clearServer(UUID player) {
        SERVER.remove(player);
    }

    public static void setClientLookup(ClientLookup lookup) {
        clientLookup = lookup;
    }

    public static boolean clientHeld(Entity entity, String key) {
        ClientLookup lookup = clientLookup;
        return lookup != null && lookup.isHeld(entity, key);
    }
}
