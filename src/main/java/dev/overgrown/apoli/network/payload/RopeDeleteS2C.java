package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record RopeDeleteS2C(UUID owner) implements CustomPacketPayload {

    public static final Type<RopeDeleteS2C> TYPE = new Type<>(Apoli.id("rope_delete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RopeDeleteS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        RopeDeleteS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
    }

    public static RopeDeleteS2C read(FriendlyByteBuf buf) {
        return new RopeDeleteS2C(buf.readUUID());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
