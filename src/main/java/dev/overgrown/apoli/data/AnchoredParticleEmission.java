package dev.overgrown.apoli.data;

import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.network.payload.AnchoredParticlesS2C;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class AnchoredParticleEmission {
    private final Entity entity;
    private final BodyPart part;
    private final Optional<Space> space;
    private final ParticleOptions options;
    private final boolean force;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final Vec3 localVelocity;
    private final int count;
    private final Vector spread;
    private final float scalarSpeed;
    @Nullable
    private AnchoredParticlesS2C payload;
    @Nullable
    private List<Packet<?>> legacy;

    public AnchoredParticleEmission(Entity entity, BodyPart part, Optional<Space> space, ParticleOptions options,
                                    boolean force, float offsetX, float offsetY, float offsetZ, Vec3 localVelocity,
                                    int count, Vector spread, float scalarSpeed) {
        this.entity = entity;
        this.part = part;
        this.space = space;
        this.options = options;
        this.force = force;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.localVelocity = localVelocity;
        this.count = count;
        this.spread = spread;
        this.scalarSpeed = scalarSpeed;
    }

    public boolean inRange(ServerPlayer player) {
        return ParticleBroadcast.inRange(player, force,
            entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
    }

    public void sendTo(ServerLevel level, ServerPlayer player) {
        if (player.level() != level) return;
        if (ApoliNetwork.canSendAnchoredParticles(player)) {
            if (payload == null) {
                payload = new AnchoredParticlesS2C(entity.getId(), part.name(), AnchoredParticlesS2C.spaceId(space),
                    offsetX, offsetY, offsetZ,
                    (float) localVelocity.x, (float) localVelocity.y, (float) localVelocity.z,
                    count, spread.x(), spread.y(), spread.z(), scalarSpeed, force, options);
            }
            ApoliNetwork.sendAnchoredParticles(player, payload);
            return;
        }
        if (legacy == null) legacy = legacyPackets(level);
        for (int i = 0; i < legacy.size(); i++) {
            ParticleBroadcast.send(level, player, legacy.get(i));
        }
    }

    private List<Packet<?>> legacyPackets(ServerLevel level) {
        ModelPartAnchor.Frame frame = ModelPartAnchor.frameOf(entity, part);
        Vec3 origin = ParticlePlacement.origin(entity, frame, space, offsetX, offsetY, offsetZ);
        Vec3 velocity = ParticlePlacement.velocity(entity, frame, space, localVelocity);
        return ParticlePlacement.packets(options, force, origin, velocity, count, spread, scalarSpeed,
            level, entity, frame, space);
    }
}
