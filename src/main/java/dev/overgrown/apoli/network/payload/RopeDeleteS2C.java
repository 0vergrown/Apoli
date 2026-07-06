package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RopeDeleteS2C(int id) {
    public static final ResourceLocation CHANNEL = Apoli.id("rope_delete");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
    }

    public static RopeDeleteS2C read(FriendlyByteBuf buf) {
        return new RopeDeleteS2C(buf.readVarInt());
    }
}
