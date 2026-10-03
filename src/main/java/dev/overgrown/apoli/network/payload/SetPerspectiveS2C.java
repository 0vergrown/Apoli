package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SetPerspectiveS2C(int perspective) {
    public static final ResourceLocation CHANNEL = Apoli.id("set_perspective");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(perspective);
    }

    public static SetPerspectiveS2C read(FriendlyByteBuf buf) {
        return new SetPerspectiveS2C(buf.readVarInt());
    }
}
