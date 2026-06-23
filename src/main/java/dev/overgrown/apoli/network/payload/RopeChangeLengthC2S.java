package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RopeChangeLengthC2S(double delta) implements CustomPacketPayload {

    public static final Type<RopeChangeLengthC2S> TYPE = new Type<>(Apoli.id("rope_change_length"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RopeChangeLengthC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        RopeChangeLengthC2S::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(delta);
    }

    public static RopeChangeLengthC2S read(FriendlyByteBuf buf) {
        return new RopeChangeLengthC2S(buf.readDouble());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
