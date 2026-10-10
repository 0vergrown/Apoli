package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.Space;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record AnchoredParticlesS2C(int entityId, String part, int space,
                                   float offsetX, float offsetY, float offsetZ,
                                   float velocityX, float velocityY, float velocityZ,
                                   int count, float spreadX, float spreadY, float spreadZ, float speed,
                                   boolean force, ParticleOptions particle) {
    public static final ResourceLocation CHANNEL = Apoli.id("anchored_particles");

    public static int spaceId(Optional<Space> space) {
        return space.map(Enum::ordinal).orElse(-1);
    }

    public Optional<Space> spaceValue() {
        Space[] values = Space.values();
        return space >= 0 && space < values.length ? Optional.of(values[space]) : Optional.empty();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(part);
        buf.writeVarInt(space + 1);
        buf.writeFloat(offsetX);
        buf.writeFloat(offsetY);
        buf.writeFloat(offsetZ);
        buf.writeFloat(velocityX);
        buf.writeFloat(velocityY);
        buf.writeFloat(velocityZ);
        buf.writeVarInt(count);
        buf.writeFloat(spreadX);
        buf.writeFloat(spreadY);
        buf.writeFloat(spreadZ);
        buf.writeFloat(speed);
        buf.writeBoolean(force);
        buf.writeId(BuiltInRegistries.PARTICLE_TYPE, particle.getType());
        particle.writeToNetwork(buf);
    }

    public static AnchoredParticlesS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        String part = buf.readUtf();
        int space = buf.readVarInt() - 1;
        float offsetX = buf.readFloat();
        float offsetY = buf.readFloat();
        float offsetZ = buf.readFloat();
        float velocityX = buf.readFloat();
        float velocityY = buf.readFloat();
        float velocityZ = buf.readFloat();
        int count = buf.readVarInt();
        float spreadX = buf.readFloat();
        float spreadY = buf.readFloat();
        float spreadZ = buf.readFloat();
        float speed = buf.readFloat();
        boolean force = buf.readBoolean();
        ParticleOptions particle = readParticle(buf, buf.readById(BuiltInRegistries.PARTICLE_TYPE));
        return new AnchoredParticlesS2C(entityId, part, space, offsetX, offsetY, offsetZ,
            velocityX, velocityY, velocityZ, count, spreadX, spreadY, spreadZ, speed, force, particle);
    }

    private static <T extends ParticleOptions> T readParticle(FriendlyByteBuf buf, ParticleType<T> type) {
        return type.getDeserializer().fromNetwork(type, buf);
    }
}
