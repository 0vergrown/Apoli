package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CameraTypeC2S(int cameraType) implements CustomPacketPayload {
    public static final Type<CameraTypeC2S> TYPE = new Type<>(Apoli.id("camera_type"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CameraTypeC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        CameraTypeC2S::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(cameraType);
    }

    public static CameraTypeC2S read(FriendlyByteBuf buf) {
        return new CameraTypeC2S(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
