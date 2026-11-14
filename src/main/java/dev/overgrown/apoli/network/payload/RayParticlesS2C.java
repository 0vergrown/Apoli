package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RayParticlesS2C(ParticleOptions particle, double x, double y, double z,
                              float dirX, float dirY, float dirZ, float length, float spacing) {
    public static final ResourceLocation CHANNEL = Apoli.id("ray_particles");

    public void write(FriendlyByteBuf buf) {
        buf.writeId(BuiltInRegistries.PARTICLE_TYPE, particle.getType());
        particle.writeToNetwork(buf);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(dirX);
        buf.writeFloat(dirY);
        buf.writeFloat(dirZ);
        buf.writeFloat(length);
        buf.writeFloat(spacing);
    }

    public static RayParticlesS2C read(FriendlyByteBuf buf) {
        ParticleOptions particle = readParticle(buf, buf.readById(BuiltInRegistries.PARTICLE_TYPE));
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

    private static <T extends ParticleOptions> T readParticle(FriendlyByteBuf buf, ParticleType<T> type) {
        return type.getDeserializer().fromNetwork(type, buf);
    }
}
