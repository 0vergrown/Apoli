package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.Space;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

public record AnchoredParticlesS2C(int entityId, String part, int space,
                                   float offsetX, float offsetY, float offsetZ,
                                   float velocityX, float velocityY, float velocityZ,
                                   int count, float spreadX, float spreadY, float spreadZ, float speed,
                                   boolean force, ParticleOptions particle) implements CustomPacketPayload {
    public static final Type<AnchoredParticlesS2C> TYPE = new Type<>(Apoli.id("anchored_particles"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnchoredParticlesS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        AnchoredParticlesS2C::read);

    public static int spaceId(Optional<Space> space) {
        return space.map(Enum::ordinal).orElse(-1);
    }

    public Optional<Space> spaceValue() {
        Space[] values = Space.values();
        return space >= 0 && space < values.length ? Optional.of(values[space]) : Optional.empty();
    }

    public void write(RegistryFriendlyByteBuf buf) {
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
        ParticleTypes.STREAM_CODEC.encode(buf, particle);
    }

    public static AnchoredParticlesS2C read(RegistryFriendlyByteBuf buf) {
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
        ParticleOptions particle = ParticleTypes.STREAM_CODEC.decode(buf);
        return new AnchoredParticlesS2C(entityId, part, space, offsetX, offsetY, offsetZ,
            velocityX, velocityY, velocityZ, count, spreadX, spreadY, spreadZ, speed, force, particle);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
