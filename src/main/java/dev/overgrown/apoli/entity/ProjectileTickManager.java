package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProjectileTickManager {

    private static final int MISSING_GRACE_TICKS = 20;

    private static final class Tracked {
        private final UUID projectileId;
        private final UUID ownerId;
        private final BiEntityAction action;
        private Entity projectile;
        private Entity owner;
        private int missingTicks;

        private Tracked(Entity projectile, Entity owner, BiEntityAction action) {
            this.projectileId = projectile.getUUID();
            this.ownerId = owner == null ? null : owner.getUUID();
            this.action = action;
            this.projectile = projectile;
            this.owner = owner;
        }
    }

    private static final List<Tracked> TRACKED = new ArrayList<>();

    private static boolean loggedActionFailure;

    private ProjectileTickManager() {}

    public static void track(Entity projectile, Entity owner, BiEntityAction action) {
        if (projectile == null || action == null) return;
        TRACKED.add(new Tracked(projectile, owner, action));
    }

    public static void tick(MinecraftServer server) {
        if (TRACKED.isEmpty()) return;
        for (int i = TRACKED.size() - 1; i >= 0; i--) {
            Tracked t = TRACKED.get(i);
            Entity projectile = t.projectile;
            if (projectile == null || projectile.isRemoved()) {
                projectile = find(server, t.projectileId);
                t.projectile = projectile;
            }
            if (projectile == null || !(projectile.level() instanceof ServerLevel level)) {
                if (++t.missingTicks > MISSING_GRACE_TICKS) drop(i);
                continue;
            }
            t.missingTicks = 0;
            Entity owner = t.owner;
            if (t.ownerId != null && (owner == null || owner.isRemoved())) {
                owner = find(server, t.ownerId);
                t.owner = owner;
            }
            try {
                t.action.run(BiEntityCtx.of(owner, projectile, level));
            } catch (Exception e) {
                drop(i);
                if (!loggedActionFailure) {
                    loggedActionFailure = true;
                    Apoli.LOGGER.error("[Apoli] tick_bientity_action failed on a fired projectile; that "
                        + "projectile stopped ticking. Later failures are not logged.", e);
                }
            }
        }
    }

    public static void clearAll() {
        TRACKED.clear();
        loggedActionFailure = false;
    }

    private static void drop(int index) {
        int last = TRACKED.size() - 1;
        if (index != last) TRACKED.set(index, TRACKED.get(last));
        TRACKED.remove(last);
    }

    private static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) return entity;
        }
        return null;
    }
}
