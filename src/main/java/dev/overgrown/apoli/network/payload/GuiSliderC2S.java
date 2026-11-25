package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.gui.GuiMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record GuiSliderC2S(int containerId, int slider, double value, boolean released) {
    public static final ResourceLocation CHANNEL = Apoli.id("gui_slider");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(slider);
        buf.writeDouble(value);
        buf.writeBoolean(released);
    }

    public static GuiSliderC2S read(FriendlyByteBuf buf) {
        return new GuiSliderC2S(buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readBoolean());
    }

    public void handle(ServerPlayer player) {
        if (player.isSpectator() || !(player.containerMenu instanceof GuiMenu menu) || menu.containerId != containerId) return;
        if (!menu.stillValid(player)) return;
        menu.slide(slider, value, released);
        menu.broadcastChanges();
    }
}
