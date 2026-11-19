package dev.overgrown.apoli.entity.possession;

import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.network.payload.PossessionSyncS2C;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PossessionManager {

    private record Session(ServerPlayer possessor, Mob host, long endTick, @Nullable BiEntityAction endAction,
                           boolean hostAttacks) {}

    private static final Map<UUID, Session> BY_POSSESSOR = new HashMap<>();
    private static final List<Session> ENDING = new ArrayList<>();

    private PossessionManager() {}

    public static boolean canPossess(ServerPlayer possessor, Entity target) {
        if (!(target instanceof Mob host) || host instanceof EnderDragon) return false;
        if (possessor.isRemoved() || !possessor.isAlive() || possessor.isSpectator()) return false;
        if (host.isRemoved() || !host.isAlive()) return false;
        if (host.level() != possessor.level()) return false;
        Entity current = Possession.possessorOf(host);
        return current == null || current == possessor;
    }

    public static boolean start(ServerPlayer possessor, Mob host, int duration, @Nullable BiEntityAction endAction,
                                boolean hostAttacks) {
        if (!canPossess(possessor, host)) return false;
        long endTick = duration > 0 ? possessor.server.overworld().getGameTime() + duration : -1L;
        Session existing = BY_POSSESSOR.get(possessor.getUUID());
        if (existing != null) {
            if (existing.host() == host) {
                BY_POSSESSOR.put(possessor.getUUID(), new Session(possessor, host, endTick, endAction, hostAttacks));
                return true;
            }
            finish(existing);
            if (!canPossess(possessor, host)) return false;
        }
        possessor.stopRiding();
        host.stopRiding();
        host.setTarget(null);
        host.getNavigation().stop();
        host.setXxa(0.0f);
        host.setYya(0.0f);
        host.setZza(0.0f);
        host.setJumping(false);
        BY_POSSESSOR.put(possessor.getUUID(), new Session(possessor, host, endTick, endAction, hostAttacks));
        Possession.link(possessor, host);
        possessor.connection.teleport(host.getX(), host.getY(), host.getZ(), host.getYRot(), host.getXRot());
        possessor.resetFallDistance();
        possessor.refreshDimensions();
        ApoliNetwork.broadcastPossession(possessor, host, new PossessionSyncS2C(possessor.getId(), host.getId()));
        return true;
    }

    public static void end(Entity entity) {
        Session session = sessionOf(entity);
        if (session != null) finish(session);
    }

    public static boolean hostAttacks(Player possessor) {
        Session session = BY_POSSESSOR.get(possessor.getUUID());
        return session != null && session.hostAttacks()
            && session.host().getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE);
    }

    public static void tick(MinecraftServer server) {
        if (BY_POSSESSOR.isEmpty()) return;
        long now = server.overworld().getGameTime();
        for (Session session : BY_POSSESSOR.values()) {
            ServerPlayer possessor = session.possessor();
            Mob host = session.host();
            if (possessor.isRemoved() || !possessor.isAlive() || possessor.isSpectator()
                || host.isRemoved() || !host.isAlive() || possessor.level() != host.level()
                || Possession.hostOf(possessor) != host
                || (session.endTick() >= 0 && now >= session.endTick())) {
                ENDING.add(session);
            }
        }
        if (ENDING.isEmpty()) return;
        for (Session session : ENDING) finish(session);
        ENDING.clear();
    }

    public static void syncTo(ServerPlayer viewer, Entity tracked) {
        Entity host = Possession.hostOf(tracked);
        if (host != null) {
            ApoliNetwork.sendPossession(viewer, new PossessionSyncS2C(tracked.getId(), host.getId()));
        }
        Entity possessor = Possession.possessorOf(tracked);
        if (possessor != null) {
            ApoliNetwork.sendPossession(viewer, new PossessionSyncS2C(possessor.getId(), tracked.getId()));
        }
    }

    public static void clearAll() {
        for (Session session : BY_POSSESSOR.values()) Possession.unlink(session.possessor());
        BY_POSSESSOR.clear();
        ENDING.clear();
    }

    @Nullable
    private static Session sessionOf(Entity entity) {
        Session session = BY_POSSESSOR.get(entity.getUUID());
        if (session != null) return session;
        Entity possessor = Possession.possessorOf(entity);
        return possessor == null ? null : BY_POSSESSOR.get(possessor.getUUID());
    }

    private static void finish(Session session) {
        if (BY_POSSESSOR.remove(session.possessor().getUUID()) != session) return;
        ServerPlayer possessor = session.possessor();
        Mob host = session.host();
        Possession.unlink(possessor);
        possessor.refreshDimensions();
        host.resetFallDistance();
        host.setDeltaMovement(Vec3.ZERO);
        host.setShiftKeyDown(false);
        host.setSprinting(false);
        ApoliNetwork.broadcastPossession(possessor, host, new PossessionSyncS2C(possessor.getId(), -1));
        if (session.endAction() != null && !(possessor.isRemoved() && host.isRemoved())) {
            session.endAction().run(new BiEntityCtx(possessor, host, possessor.level()));
        }
    }
}
