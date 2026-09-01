package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ScrollWheelC2S(boolean up, int notches) {
    public static final int MAX_NOTCHES = 16;
    public static final ResourceLocation CHANNEL = Apoli.id("scroll_wheel");

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(up);
        buf.writeVarInt(notches);
    }

    public static ScrollWheelC2S read(FriendlyByteBuf buf) {
        boolean up = buf.readBoolean();
        int notches = Math.max(1, Math.min(MAX_NOTCHES, buf.readVarInt()));
        return new ScrollWheelC2S(up, notches);
    }
}
