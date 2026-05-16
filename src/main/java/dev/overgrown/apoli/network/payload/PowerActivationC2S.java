package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PowerActivationC2S(ResourceLocation power) {
    public static final ResourceLocation CHANNEL = Apoli.id("power_activation");

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(power);
    }

    public static PowerActivationC2S read(FriendlyByteBuf buf) {
        return new PowerActivationC2S(buf.readResourceLocation());
    }
}