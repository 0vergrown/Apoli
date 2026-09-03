package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record DevModeS2C(boolean enabled) {

    public static final ResourceLocation CHANNEL = Apoli.id("dev_mode");

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    public static DevModeS2C read(FriendlyByteBuf buf) {
        return new DevModeS2C(buf.readBoolean());
    }
}
