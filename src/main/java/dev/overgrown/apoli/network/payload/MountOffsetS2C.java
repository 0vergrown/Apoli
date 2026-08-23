package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.Space;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record MountOffsetS2C(int passengerId, double x, double y, double z, Space space) {
    public static final ResourceLocation CHANNEL = Apoli.id("mount_offset");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(passengerId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeEnum(space);
    }

    public static MountOffsetS2C read(FriendlyByteBuf buf) {
        return new MountOffsetS2C(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readEnum(Space.class));
    }
}
