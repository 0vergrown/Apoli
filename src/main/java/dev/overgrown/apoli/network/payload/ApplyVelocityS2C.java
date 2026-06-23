package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ApplyVelocityS2C(int entityId, double x, double y, double z, boolean set) {
    public static final ResourceLocation CHANNEL = Apoli.id("apply_velocity");

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
}
