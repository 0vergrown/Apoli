package dev.overgrown.apoli.data;

import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.network.payload.RayParticlesS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class RayParticles {
    private static final double RANGE_SQ = 32.0 * 32.0;

    private RayParticles() {}

    public static void broadcast(ServerLevel level, ParticleOptions particle, Vec3 origin, Vec3 dir,
                                 double length, double spacing) {
        if (length <= 0.0) return;
        RayParticlesS2C payload = null;
        List<Vec3> points = null;
        List<Packet<?>> packets = null;
        for (ServerPlayer player : level.players()) {
            if (!ApoliNetwork.connected(player) || !inRange(player, origin, dir, length)) continue;
            if (ApoliNetwork.canSendRayParticles(player)) {
                if (payload == null) {
                    payload = new RayParticlesS2C(particle, origin.x, origin.y, origin.z,
                        (float) dir.x, (float) dir.y, (float) dir.z, (float) length, (float) spacing);
                }
                ApoliNetwork.sendRayParticles(player, payload);
                continue;
            }
            if (points == null) {
                points = new ArrayList<>();
                packets = new ArrayList<>();
                for (double t = 0; t < length; t += spacing) {
                    Vec3 p = origin.add(dir.scale(t));
                    points.add(p);
                    packets.add(ParticleBroadcast.packet(particle, false, p.x, p.y, p.z, 1, 0.0F, 0.0F, 0.0F, 0.0F));
                }
            }
            for (int i = 0; i < points.size(); i++) {
                Vec3 p = points.get(i);
                if (ParticleBroadcast.inRange(player, false, p.x, p.y, p.z)) {
                    ParticleBroadcast.send(level, player, packets.get(i));
                }
            }
        }
    }

    private static boolean inRange(ServerPlayer player, Vec3 origin, Vec3 dir, double length) {
        BlockPos at = player.blockPosition();
        double px = at.getX() + 0.5 - origin.x;
        double py = at.getY() + 0.5 - origin.y;
        double pz = at.getZ() + 0.5 - origin.z;
        double t = Mth.clamp(px * dir.x + py * dir.y + pz * dir.z, 0.0, length);
        double dx = px - dir.x * t;
        double dy = py - dir.y * t;
        double dz = pz - dir.z * t;
        return dx * dx + dy * dy + dz * dz < RANGE_SQ;
    }
}
