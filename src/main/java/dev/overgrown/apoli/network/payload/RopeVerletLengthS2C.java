package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RopeVerletLengthS2C(int id, double length) implements CustomPacketPayload {

    public static final Type<RopeVerletLengthS2C> TYPE = new Type<>(Apoli.id("rope_verlet_length"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RopeVerletLengthS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        RopeVerletLengthS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
        buf.writeDouble(length);
    }

    public static RopeVerletLengthS2C read(FriendlyByteBuf buf) {
        return new RopeVerletLengthS2C(buf.readVarInt(), buf.readDouble());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
