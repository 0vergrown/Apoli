package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetPerspectiveS2C(int perspective) implements CustomPacketPayload {
    public static final Type<SetPerspectiveS2C> TYPE = new Type<>(Apoli.id("set_perspective"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPerspectiveS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        SetPerspectiveS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(perspective);
    }

    public static SetPerspectiveS2C read(FriendlyByteBuf buf) {
        return new SetPerspectiveS2C(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
