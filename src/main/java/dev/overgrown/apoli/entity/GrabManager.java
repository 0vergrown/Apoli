package dev.overgrown.apoli.entity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GrabManager {

    private record Grab(UUID grabber, UUID grabbed, double distance, long expiryTick,
                        boolean disableGrabber, boolean disableGrabbed,
                        boolean horizontalOnly, boolean verticalOnly, float lockedYaw) {}

    private static final Map<UUID, Grab> BY_GRABBED = new HashMap<>();
    private static final Set<UUID> KEYBINDS_DISABLED = new HashSet<>();
    private static final double MIN_HOLD_CLEARANCE = 0.5;

    private GrabManager() {}

    public static void start(Entity grabber, Entity grabbed, int duration, double distance,
                             boolean disableGrabber, boolean disableGrabbed,
                             boolean horizontalOnly, boolean verticalOnly) {
        if (grabber.level().isClientSide() || grabber == grabbed) return;
        long expiry = duration > 0 ? grabber.level().getGameTime() + duration : -1L;
        BY_GRABBED.put(grabbed.getUUID(), new Grab(grabber.getUUID(), grabbed.getUUID(), distance, expiry,
            disableGrabber, disableGrabbed, horizontalOnly, verticalOnly, grabber.getYRot()));
        rebuildDisabled();
    }

    public static void release(UUID entity) {
        boolean changed = BY_GRABBED.remove(entity) != null;
        changed |= BY_GRABBED.values().removeIf(grab -> grab.grabber().equals(entity));
        if (changed) rebuildDisabled();
    }

    public static boolean isGrabbed(UUID entity) {
        return BY_GRABBED.containsKey(entity);
    }

    public static boolean keybindsDisabled(UUID entity) {
        return !KEYBINDS_DISABLED.isEmpty() && KEYBINDS_DISABLED.contains(entity);
    }

    public static void tick(MinecraftServer server) {
        if (BY_GRABBED.isEmpty()) return;
        boolean changed = false;
        Iterator<Grab> it = BY_GRABBED.values().iterator();
        while (it.hasNext()) {
            Grab grab = it.next();
            Entity grabberEntity = find(server, grab.grabber());
            Entity grabbedEntity = find(server, grab.grabbed());
            if (grabberEntity == null || grabbedEntity == null
                || !grabberEntity.isAlive() || !grabbedEntity.isAlive()
                || grabberEntity.level() != grabbedEntity.level()
                || (grab.expiryTick() >= 0 && server.overworld().getGameTime() >= grab.expiryTick())) {
                it.remove();
                changed = true;
                continue;
            }
            float yaw = grab.verticalOnly() ? grab.lockedYaw() : grabberEntity.getYRot();
            float pitch = grab.horizontalOnly() ? 0.0f : grabberEntity.getXRot();
            Vec3 look = Vec3.directionFromRotation(pitch, yaw);
            Vec3 eye = grabberEntity.getEyePosition();
            Vec3 current = grabbedEntity.position();
            double holdY = Math.max(
                eye.y + look.y * grab.distance() - grabbedEntity.getBbHeight() / 2.0,
                grabberEntity.getY() + MIN_HOLD_CLEARANCE);
            Vec3 delta = new Vec3(
                eye.x + look.x * grab.distance() - current.x,
                holdY - current.y,
                eye.z + look.z * grab.distance() - current.z);
            Vec3 allowed = Entity.collideBoundingBox(grabbedEntity, delta, grabbedEntity.getBoundingBox(),
                grabbedEntity.level(), List.of());
            double x = current.x + allowed.x;
            double y = current.y + allowed.y;
            double z = current.z + allowed.z;
            grabbedEntity.fallDistance = 0.0f;
            grabbedEntity.setDeltaMovement(Vec3.ZERO);
            if (grabbedEntity instanceof ServerPlayer player) {
                player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
            } else {
                grabbedEntity.setPos(x, y, z);
            }
        }
        if (changed) rebuildDisabled();
    }

    private static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) return entity;
        }
        return null;
    }

    private static void rebuildDisabled() {
        KEYBINDS_DISABLED.clear();
        for (Grab grab : BY_GRABBED.values()) {
            if (grab.disableGrabber()) KEYBINDS_DISABLED.add(grab.grabber());
            if (grab.disableGrabbed()) KEYBINDS_DISABLED.add(grab.grabbed());
        }
    }

    public static void clearAll() {
        BY_GRABBED.clear();
        KEYBINDS_DISABLED.clear();
    }
}
