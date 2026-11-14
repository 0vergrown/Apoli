package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RayParticlesS2C(ParticleOptions particle, double x, double y, double z,
                              float dirX, float dirY, float dirZ, float length, float spacing)
    implements CustomPacketPayload {
    public static final Type<RayParticlesS2C> TYPE = new Type<>(Apoli.id("ray_particles"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RayParticlesS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        RayParticlesS2C::read);

    public void write(RegistryFriendlyByteBuf buf) {
        ParticleTypes.STREAM_CODEC.encode(buf, particle);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(dirX);
        buf.writeFloat(dirY);
        buf.writeFloat(dirZ);
        buf.writeFloat(length);
        buf.writeFloat(spacing);
    }

    public static RayParticlesS2C read(RegistryFriendlyByteBuf buf) {
        ParticleOptions particle = ParticleTypes.STREAM_CODEC.decode(buf);
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float dirX = buf.readFloat();
        float dirY = buf.readFloat();
        float dirZ = buf.readFloat();
        float length = buf.readFloat();
        float spacing = buf.readFloat();
        return new RayParticlesS2C(particle, x, y, z, dirX, dirY, dirZ, length, spacing);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
