package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.mount.MountRotation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MountOffsetS2C(int passengerId, double x, double y, double z, Space space,
                             MountRotation rotation) implements CustomPacketPayload {
    public static final Type<MountOffsetS2C> TYPE = new Type<>(Apoli.id("mount_offset"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MountOffsetS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        MountOffsetS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(passengerId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeEnum(space);
        buf.writeEnum(rotation);
    }

    public static MountOffsetS2C read(FriendlyByteBuf buf) {
        int passengerId = buf.readVarInt();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        Space space = buf.readEnum(Space.class);
        MountRotation rotation = buf.isReadable() ? buf.readEnum(MountRotation.class) : MountRotation.HEAD;
        return new MountOffsetS2C(passengerId, x, y, z, space, rotation);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
