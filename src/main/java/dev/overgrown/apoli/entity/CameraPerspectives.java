package dev.overgrown.apoli.entity;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CameraPerspectives {

    private static final Map<UUID, Boolean> KNOWN = new ConcurrentHashMap<>();

    private CameraPerspectives() {}

    public static void set(UUID player, boolean firstPerson) {
        KNOWN.put(player, firstPerson);
    }

    public static void remove(UUID player) {
        KNOWN.remove(player);
    }

    public static void clear() {
        KNOWN.clear();
    }

    public static boolean isFirstPerson(Entity entity) {
        Boolean known = KNOWN.get(entity.getUUID());
        return known == null || known;
    }
}
