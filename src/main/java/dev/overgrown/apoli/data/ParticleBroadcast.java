package dev.overgrown.apoli.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ParticleBroadcast {
    private ParticleBroadcast() {}

    public static Packet<?> packet(ParticleOptions options, boolean force, double x, double y, double z,
                                   int count, float spreadX, float spreadY, float spreadZ, float speed) {
        return new ClientboundLevelParticlesPacket(options, force, x, y, z,
            spreadX, spreadY, spreadZ, speed, count);
    }

    public static void send(ServerLevel level, ServerPlayer player, Packet<?> packet) {
        if (player.level() != level) return;
        player.connection.send(packet);
    }

    public static boolean inRange(ServerPlayer player, boolean force, double x, double y, double z) {
        BlockPos at = player.blockPosition();
        double dx = at.getX() + 0.5 - x;
        double dy = at.getY() + 0.5 - y;
        double dz = at.getZ() + 0.5 - z;
        double limit = force ? 512.0 : 32.0;
        return dx * dx + dy * dy + dz * dz < limit * limit;
    }
}
