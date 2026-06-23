package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record RopeVerletLengthS2C(UUID owner, double length) {
    public static final ResourceLocation CHANNEL = Apoli.id("rope_verlet_length");

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeDouble(length);
    }

    public static RopeVerletLengthS2C read(FriendlyByteBuf buf) {
        return new RopeVerletLengthS2C(buf.readUUID(), buf.readDouble());
    }
}
