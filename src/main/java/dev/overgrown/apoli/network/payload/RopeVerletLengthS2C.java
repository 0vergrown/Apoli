package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RopeVerletLengthS2C(int id, double length) {
    public static final ResourceLocation CHANNEL = Apoli.id("rope_verlet_length");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
        buf.writeDouble(length);
    }

    public static RopeVerletLengthS2C read(FriendlyByteBuf buf) {
        return new RopeVerletLengthS2C(buf.readVarInt(), buf.readDouble());
    }
}
