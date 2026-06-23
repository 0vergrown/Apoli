package dev.overgrown.apoli.power;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class PoweredEntities {
    private static final Set<Entity> POWERED = Collections.newSetFromMap(new IdentityHashMap<>());

    private PoweredEntities() {}

    public static void register(Entity entity) {
        if (entity != null && !entity.level().isClientSide()) POWERED.add(entity);
    }

    public static void unregister(Entity entity) {
        if (entity != null) POWERED.remove(entity);
    }

    public static void clear() {
        POWERED.clear();
    }

    public static void forEach(Consumer<Entity> action) {
        if (POWERED.isEmpty()) return;
        List<Entity> snapshot = new ArrayList<>(POWERED);
        for (Entity e : snapshot) {
            if (e.isRemoved() || !(e.level() instanceof ServerLevel)) {
                POWERED.remove(e);
                continue;
            }
            action.accept(e);
        }
    }
}
