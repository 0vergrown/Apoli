package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ScrollWheelC2S(boolean up, int notches) implements CustomPacketPayload {
    public static final Type<ScrollWheelC2S> TYPE = new Type<>(Apoli.id("scroll_wheel"));

    public static final int MAX_NOTCHES = 16;

    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollWheelC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        ScrollWheelC2S::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(up);
        buf.writeVarInt(notches);
    }

    public static ScrollWheelC2S read(FriendlyByteBuf buf) {
        boolean up = buf.readBoolean();
        int notches = Math.max(1, Math.min(MAX_NOTCHES, buf.readVarInt()));
        return new ScrollWheelC2S(up, notches);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
