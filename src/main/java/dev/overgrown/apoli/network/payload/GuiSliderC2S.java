package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.gui.GuiMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record GuiSliderC2S(int containerId, int slider, double value, boolean released) implements CustomPacketPayload {
    public static final Type<GuiSliderC2S> TYPE = new Type<>(Apoli.id("gui_slider"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuiSliderC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        GuiSliderC2S::read);

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
