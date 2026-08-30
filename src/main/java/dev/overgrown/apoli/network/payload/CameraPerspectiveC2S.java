package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CameraPerspectiveC2S(boolean firstPerson) implements CustomPacketPayload {
    public static final Type<CameraPerspectiveC2S> TYPE = new Type<>(Apoli.id("camera_perspective"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CameraPerspectiveC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        CameraPerspectiveC2S::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(firstPerson);
    }

    public static CameraPerspectiveC2S read(FriendlyByteBuf buf) {
        return new CameraPerspectiveC2S(buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
