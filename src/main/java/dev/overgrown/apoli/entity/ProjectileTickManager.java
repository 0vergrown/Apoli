package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public final class ProjectileTickManager {

    private record Tracked(Entity projectile, Entity owner, BiEntityAction action) {}

    private static final List<Tracked> TRACKED = new ArrayList<>();

    private static final List<Tracked> BATCH = new ArrayList<>();

    private ProjectileTickManager() {}

    public static void track(Entity projectile, Entity owner, BiEntityAction action) {
        if (projectile == null || action == null) return;
        TRACKED.add(new Tracked(projectile, owner, action));
    }

    public static void tick(MinecraftServer server) {
        if (TRACKED.isEmpty()) return;
        BATCH.clear();
        BATCH.addAll(TRACKED);
        TRACKED.clear();
        for (int i = 0; i < BATCH.size(); i++) {
            Tracked t = BATCH.get(i);
            Entity projectile = t.projectile();
            if (projectile.isRemoved() || !(projectile.level() instanceof ServerLevel level)) continue;
            Entity owner = t.owner();
            if (owner != null && owner.isRemoved()) owner = null;
            t.action().run(BiEntityCtx.of(owner, projectile, level));
            TRACKED.add(t);
        }
        BATCH.clear();
    }

    public static void clearAll() {
        TRACKED.clear();
        BATCH.clear();
    }
}
