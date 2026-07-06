package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RopeChangeLengthC2S(int ropeId, double delta) {
    public static final ResourceLocation CHANNEL = Apoli.id("rope_change_length");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(ropeId);
        buf.writeDouble(delta);
    }

    public static RopeChangeLengthC2S read(FriendlyByteBuf buf) {
        return new RopeChangeLengthC2S(buf.readVarInt(), buf.readDouble());
    }
}
