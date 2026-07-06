package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RopeDeleteS2C(int id) implements CustomPacketPayload {

    public static final Type<RopeDeleteS2C> TYPE = new Type<>(Apoli.id("rope_delete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RopeDeleteS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        RopeDeleteS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
    }

    public static RopeDeleteS2C read(FriendlyByteBuf buf) {
        return new RopeDeleteS2C(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
