package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ScaleSyncS2C(int entityId, byte[] data) implements CustomPacketPayload {
    public static final Type<ScaleSyncS2C> TYPE = new Type<>(Apoli.id("scale_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScaleSyncS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        ScaleSyncS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeByteArray(data);
    }

    public static ScaleSyncS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        return new ScaleSyncS2C(entityId, buf.readByteArray(8192));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
