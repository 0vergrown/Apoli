package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PowerToggleC2S(ResourceLocation power) {
    public static final ResourceLocation CHANNEL = Apoli.id("power_toggle");

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(power);
    }

    public static PowerToggleC2S read(FriendlyByteBuf buf) {
        return new PowerToggleC2S(buf.readResourceLocation());
    }
}
