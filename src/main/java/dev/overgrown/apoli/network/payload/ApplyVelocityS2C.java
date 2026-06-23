package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ApplyVelocityS2C(int entityId, double x, double y, double z, boolean set) implements CustomPacketPayload {
    public static final Type<ApplyVelocityS2C> TYPE = new Type<>(Apoli.id("apply_velocity"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyVelocityS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        ApplyVelocityS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeBoolean(set);
    }

    public static ApplyVelocityS2C read(FriendlyByteBuf buf) {
        return new ApplyVelocityS2C(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
