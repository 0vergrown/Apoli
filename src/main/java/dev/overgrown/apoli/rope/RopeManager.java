package dev.overgrown.apoli.rope;

import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.network.payload.RopeCreateS2C;
import dev.overgrown.apoli.network.payload.RopeDeleteS2C;
import dev.overgrown.apoli.network.payload.RopeVerletLengthS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dev.overgrown.apoli.rope.RopeConstants.*;

public final class RopeManager {
    private RopeManager() {}

    private static final Map<Integer, Rope> BY_ID = new HashMap<>();
    private static final Map<UUID, Set<Integer>> BY_ENTITY = new HashMap<>();
    private static final Map<UUID, Set<Integer>> BY_OWNER = new HashMap<>();
    private static int nextId = 1;
    private static @Nullable MinecraftServer server;

    public static Rope create(ServerLevel level, RopeAnchor from, RopeAnchor to, @Nullable String slot,
                              @Nullable UUID owner, RopeParams params, ResourceLocation texture) {
        server = level.getServer();
        if (slot != null && owner != null) removeMatching(owner, slot, null);

        UUID fromEntity = from.entity(level) != null ? from.entity(level).getUUID() : null;
        UUID toEntity = to.entity(level) != null ? to.entity(level).getUUID() : null;

        Vec3 pa = from.position(level);
        Vec3 pb = to.position(level);
        double dist = (pa != null && pb != null) ? pa.distanceTo(pb) : params.maxLength();
        double length = params.initialLength(dist);

        int id = nextId++;
        Rope rope = new Rope(id, from, to, fromEntity, toEntity, slot, owner, params, texture,
            level.dimension(), length);

        if (params.controllable() && owner != null) {
            Player p = level.getServer().getPlayerList().getPlayer(owner);
            if (p != null && p.isFallFlying()) {
                if (p.isShiftKeyDown()) p.stopFallFlying();
                else rope.length = Mth.clamp(rope.length - ELYTRA_LENGTH_MOD, params.minLength(), params.maxLength());
            }
        }

        BY_ID.put(id, rope);
        if (fromEntity != null) BY_ENTITY.computeIfAbsent(fromEntity, k -> new HashSet<>()).add(id);
        if (toEntity != null) BY_ENTITY.computeIfAbsent(toEntity, k -> new HashSet<>()).add(id);
        if (owner != null) BY_OWNER.computeIfAbsent(owner, k -> new HashSet<>()).add(id);

        ApoliNetwork.broadcastRopeCreate(level,
            new RopeCreateS2C(id, from, to, rope.length, params.maxLength(), texture, owner, params.controllable()));
        return rope;
    }

    public static void remove(int id) {
        Rope rope = BY_ID.remove(id);
        if (rope == null) return;
        unindex(BY_ENTITY, rope.fromEntity, id);
        unindex(BY_ENTITY, rope.toEntity, id);
        unindex(BY_OWNER, rope.owner, id);
        ServerLevel level = serverLevel(rope);
        if (level != null) ApoliNetwork.broadcastRopeDelete(level, new RopeDeleteS2C(id));
    }

    public static int removeMatching(@Nullable UUID owner, @Nullable String slot, @Nullable UUID attachedTo) {
        List<Integer> doomed = new ArrayList<>();
        for (Rope rope : BY_ID.values()) {
            if (owner != null && !owner.equals(rope.owner)) continue;
            if (slot != null && !slot.equals(rope.slot)) continue;
            if (attachedTo != null && !rope.touches(attachedTo)) continue;
            doomed.add(rope.id);
        }
        doomed.forEach(RopeManager::remove);
        return doomed.size();
    }

    public static boolean detachControllable(UUID owner) {
        List<Integer> doomed = new ArrayList<>();
        for (Rope rope : forOwner(owner)) if (rope.params.controllable()) doomed.add(rope.id);
        doomed.forEach(RopeManager::remove);
        return !doomed.isEmpty();
    }

    public static void onEntityGone(UUID entity) {
        Set<Integer> ids = BY_ENTITY.get(entity);
        if (ids == null) return;
        for (int id : new ArrayList<>(ids)) remove(id);
    }

    public static void clear() {
        BY_ID.clear();
        BY_ENTITY.clear();
        BY_OWNER.clear();
        server = null;
    }

    public static @Nullable Rope get(int id) {
        return BY_ID.get(id);
    }

    public static List<Rope> forEntity(UUID entity) {
        Set<Integer> ids = BY_ENTITY.get(entity);
        if (ids == null) return List.of();
        List<Rope> out = new ArrayList<>(ids.size());
        for (int id : ids) { Rope r = BY_ID.get(id); if (r != null) out.add(r); }
        return out;
    }

    public static List<Rope> forOwner(UUID owner) {
        Set<Integer> ids = BY_OWNER.get(owner);
        if (ids == null) return List.of();
        List<Rope> out = new ArrayList<>(ids.size());
        for (int id : ids) { Rope r = BY_ID.get(id); if (r != null) out.add(r); }
        return out;
    }

    public static boolean connected(UUID a, UUID b) {
        for (Rope rope : forEntity(a)) if (rope.touches(b)) return true;
        return false;
    }

    public static @Nullable Rope controllableRopeOf(UUID owner) {
        for (Rope rope : forOwner(owner)) if (rope.params.controllable()) return rope;
        return null;
    }

    public static void handleLengthChange(ServerPlayer player, int ropeId, double delta) {
        Rope rope = BY_ID.get(ropeId);
        if (rope == null || !player.getUUID().equals(rope.owner)) return;
        ServerLevel level = serverLevel(rope);
        if (level == null) return;

        double step = Math.signum(delta) * rope.params.reelStep();
        Vec3 pa = rope.from.position(level);
        Vec3 pb = rope.to.position(level);
        if (pa != null && pb != null) {
            double dist = pa.distanceTo(pb);
            if (step < 0 && dist < 0.95 * rope.length) step *= rope.params.slackPullRate();
        }
        rope.length = Mth.clamp(rope.length + step, rope.params.minLength(), rope.params.maxLength());
        ApoliNetwork.broadcastRopeVerletLength(level, new RopeVerletLengthS2C(ropeId, rope.length));
    }

    public static void reel(int ropeId, double amount) {
        Rope rope = BY_ID.get(ropeId);
        if (rope == null) return;
        rope.length = Mth.clamp(rope.length - amount, rope.params.minLength(), rope.params.maxLength());
        ServerLevel level = serverLevel(rope);
        if (level != null) ApoliNetwork.broadcastRopeVerletLength(level, new RopeVerletLengthS2C(ropeId, rope.length));
    }

    public static void applySwingInput(ServerPlayer player, int ropeId, Vec3 inputDir) {
        Rope rope = BY_ID.get(ropeId);
        if (rope == null || !rope.params.controllable() || !player.getUUID().equals(rope.owner)) return;
        if (player.onGround()) return;

        double x = Mth.clamp(inputDir.x, -1.0, 1.0);
        double z = Mth.clamp(inputDir.z, -1.0, 1.0);
        if (x == 0 && z == 0) return;

        float yaw = (float) Math.toRadians(player.getYRot());
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        Vec3 forward = new Vec3(-sin, 0, cos);
        Vec3 right = new Vec3(-cos, 0, -sin);
        Vec3 wish = forward.scale(z).add(right.scale(x));
        double wishSq = wish.lengthSqr();
        if (wishSq < 1.0e-6) return;
        if (wishSq > 1.0) wish = wish.scale(1.0 / Math.sqrt(wishSq));

        RopeParams p = rope.params;
        Vec3 vel = player.getDeltaMovement();
        Vec3 horizontal = new Vec3(vel.x, 0, vel.z);
        Vec3 boosted = horizontal.add(wish.scale(p.controlAccel()));
        double cap = Math.max(p.maxSwingSpeed(), horizontal.length());
        if (boosted.lengthSqr() > cap * cap) {
            boosted = boosted.normalize().scale(cap);
        }
        player.setDeltaMovement(boosted.x, vel.y, boosted.z);
        player.hurtMarked = true;
    }

    public static void tick(MinecraftServer mc) {
        server = mc;
        if (BY_ID.isEmpty()) return;
        List<Rope> ropes = new ArrayList<>(BY_ID.values());
        for (Rope rope : ropes) {
            if (!BY_ID.containsKey(rope.id)) continue;
            ServerLevel level = mc.getLevel(rope.dimension);
            if (level == null) { remove(rope.id); continue; }

            if (endpointDead(rope.from, level) || endpointDead(rope.to, level)) { remove(rope.id); continue; }

            Vec3 pa = rope.from.position(level);
            Vec3 pb = rope.to.position(level);
            if (pa == null || pb == null) { remove(rope.id); continue; }

            if (rope.params.breakBeyond() > 0 && pa.distanceTo(pb) > rope.params.breakBeyond()) {
                remove(rope.id);
                continue;
            }

            Entity ea = rope.from.entity(level);
            Entity eb = rope.to.entity(level);

            Player controller = controllerOf(rope, ea, eb);
            if (controller != null) {
                if (controller.isFallFlying()) {
                    if (++rope.flightTicks >= ELYTRA_TIME_LIMIT) { remove(rope.id); continue; }
                } else {
                    rope.flightTicks = 0;
                }
            }

            boolean moveFrom = rope.params.constrainFrom() && ea instanceof LivingEntity;
            boolean moveTo = rope.params.constrainTo() && eb instanceof LivingEntity;
            double share = (moveFrom && moveTo) ? 0.5 : 1.0;

            if (moveFrom) constrain((LivingEntity) ea, pb, rope, share, controller == ea);
            if (moveTo) constrain((LivingEntity) eb, pa, rope, share, controller == eb);
        }
    }

    private static void constrain(LivingEntity e, Vec3 anchor, Rope rope, double share, boolean controller) {
        RopeParams p = rope.params;
        Vec3 pos = e.getBoundingBox().getCenter();
        Vec3 delta = pos.subtract(anchor);
        double dist = delta.length();
        if (dist < 1e-6) return;
        Vec3 dir = delta.normalize();

        switch (p.mode()) {
            case SPRING -> {
                if (dist <= rope.length) return;
                double excess = dist - rope.length;
                e.setDeltaMovement(e.getDeltaMovement().add(limited(dir.scale(-excess * p.stiffness() * share))));
                markMoved(e);
            }
            case RIGID -> {
                double diff = dist - rope.length;
                if (Math.abs(diff) < 1.0e-4) return;
                e.setDeltaMovement(e.getDeltaMovement().add(limited(dir.scale(-diff * p.stiffness() * share))));
                markMoved(e);
            }
            default -> {
                if (dist <= rope.length) return;
                if (anchor.y > e.getY()) e.fallDistance = Math.max(0, e.fallDistance - 1.0f);
                double excess = dist - rope.length;
                Vec3 vel = e.getDeltaMovement();
                double radial = vel.dot(dir);
                Vec3 radialVel = dir.scale(radial);
                Vec3 tangentialVel = vel.subtract(radialVel);
                if (radial > 0) radialVel = radialVel.scale(p.radialDamping());
                if (controller && e instanceof Player pl && pl.isSprinting() && tangentialVel.length() < p.maxSwingSpeed()) {
                    tangentialVel = tangentialVel.normalize().scale(tangentialVel.length() * p.swingBoost());
                }
                e.setDeltaMovement(tangentialVel.add(radialVel));
                double springScale = radial < 0 ? p.springScaling() : 1.0;
                e.setDeltaMovement(e.getDeltaMovement().add(limited(dir.scale(-excess * p.stiffness() * springScale * share))));
                markMoved(e);
            }
        }
    }

    private static final double MAX_CONSTRAINT_IMPULSE = 4.0;

    private static Vec3 limited(Vec3 impulse) {
        double lengthSq = impulse.lengthSqr();
        if (lengthSq <= MAX_CONSTRAINT_IMPULSE * MAX_CONSTRAINT_IMPULSE) return impulse;
        return impulse.scale(MAX_CONSTRAINT_IMPULSE / Math.sqrt(lengthSq));
    }

    private static void markMoved(LivingEntity e) {
        if (e instanceof ServerPlayer) e.hurtMarked = true;
        else e.hasImpulse = true;
    }

    private static boolean endpointDead(RopeAnchor anchor, ServerLevel level) {
        if (!(anchor instanceof RopeAnchor.OfEntity)) return false;
        Entity e = anchor.entity(level);
        return e == null || e.isRemoved() || (e instanceof LivingEntity le && !le.isAlive());
    }

    private static @Nullable Player controllerOf(Rope rope, @Nullable Entity ea, @Nullable Entity eb) {
        if (!rope.params.controllable() || rope.owner == null) return null;
        if (ea instanceof Player p && p.getUUID().equals(rope.owner)) return p;
        if (eb instanceof Player p && p.getUUID().equals(rope.owner)) return p;
        return null;
    }

    private static @Nullable ServerLevel serverLevel(Rope rope) {
        return server == null ? null : server.getLevel(rope.dimension);
    }

    private static void unindex(Map<UUID, Set<Integer>> index, @Nullable UUID key, int id) {
        if (key == null) return;
        Set<Integer> set = index.get(key);
        if (set == null) return;
        set.remove(id);
        if (set.isEmpty()) index.remove(key);
    }
}
