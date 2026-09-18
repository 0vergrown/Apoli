package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ScaleSyncS2C(int entityId, byte[] data) {
    public static final ResourceLocation CHANNEL = Apoli.id("scale_sync");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeByteArray(data);
    }

    public static ScaleSyncS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        return new ScaleSyncS2C(entityId, buf.readByteArray(8192));
    }
}
