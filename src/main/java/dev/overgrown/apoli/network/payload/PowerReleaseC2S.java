package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PowerReleaseC2S(ResourceLocation power) {
    public static final ResourceLocation CHANNEL = Apoli.id("power_release");

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(power);
    }

    public static PowerReleaseC2S read(FriendlyByteBuf buf) {
        return new PowerReleaseC2S(buf.readResourceLocation());
    }
}
