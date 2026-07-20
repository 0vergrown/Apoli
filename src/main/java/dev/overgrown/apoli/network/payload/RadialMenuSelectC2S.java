package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RadialMenuSelectC2S(int nonce, int index) {
    public static final ResourceLocation CHANNEL = Apoli.id("radial_menu_select");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(nonce);
        buf.writeVarInt(index);
    }

    public static RadialMenuSelectC2S read(FriendlyByteBuf buf) {
        return new RadialMenuSelectC2S(buf.readVarInt(), buf.readVarInt());
    }
}
