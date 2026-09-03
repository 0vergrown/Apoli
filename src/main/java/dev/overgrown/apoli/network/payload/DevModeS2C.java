package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DevModeS2C(boolean enabled) implements CustomPacketPayload {

    public static final Type<DevModeS2C> TYPE = new Type<>(Apoli.id("dev_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DevModeS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        DevModeS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    public static DevModeS2C read(FriendlyByteBuf buf) {
        return new DevModeS2C(buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
