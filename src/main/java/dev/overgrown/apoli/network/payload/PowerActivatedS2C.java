package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PowerActivatedS2C(ResourceLocation power, int cooldown) {
    public static final ResourceLocation CHANNEL = Apoli.id("power_activated");

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(power);
        buf.writeVarInt(cooldown);
    }

    public static PowerActivatedS2C read(FriendlyByteBuf buf) {
        return new PowerActivatedS2C(buf.readResourceLocation(), buf.readVarInt());
    }
}
