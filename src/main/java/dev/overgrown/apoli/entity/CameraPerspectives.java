package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.data.CameraPerspective;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CameraPerspectives {

    private static final Map<UUID, CameraPerspective> KNOWN = new ConcurrentHashMap<>();

    private CameraPerspectives() {}

    public static void set(UUID player, boolean firstPerson) {
        CameraPerspective previous = KNOWN.get(player);
        if (firstPerson) {
            KNOWN.put(player, CameraPerspective.FIRST_PERSON);
        } else if (previous == null || previous == CameraPerspective.FIRST_PERSON) {
            KNOWN.put(player, CameraPerspective.THIRD_PERSON_BACK);
        }
    }

    public static void setType(UUID player, int cameraType) {
        KNOWN.put(player, CameraPerspective.byId(cameraType));
    }

    public static void remove(UUID player) {
        KNOWN.remove(player);
    }

    public static void clear() {
        KNOWN.clear();
    }

    public static boolean isFirstPerson(Entity entity) {
        return typeOf(entity) == CameraPerspective.FIRST_PERSON;
    }

    public static CameraPerspective typeOf(Entity entity) {
        CameraPerspective known = KNOWN.get(entity.getUUID());
        return known == null ? CameraPerspective.FIRST_PERSON : known;
    }
}
