package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.TextBar;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record TextDisplayS2C(TextBar bar, Component text, int fadeIn, int stay, int fadeOut) {
    public static final ResourceLocation CHANNEL = Apoli.id("text_display");

    public void write(FriendlyByteBuf buf) {
        buf.writeByte(bar.ordinal());
        buf.writeComponent(text);
        buf.writeVarInt(fadeIn);
        buf.writeVarInt(stay);
        buf.writeVarInt(fadeOut);
    }

    public static TextDisplayS2C read(FriendlyByteBuf buf) {
        TextBar bar = TextBar.VALUES[Math.floorMod(buf.readByte(), TextBar.VALUES.length)];
        Component text = buf.readComponent();
        int fadeIn = buf.readVarInt();
        int stay = buf.readVarInt();
        int fadeOut = buf.readVarInt();
        return new TextDisplayS2C(bar, text, fadeIn, stay, fadeOut);
    }
}
