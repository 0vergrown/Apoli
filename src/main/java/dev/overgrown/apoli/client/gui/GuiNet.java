package dev.overgrown.apoli.client.gui;

import dev.overgrown.apoli.network.payload.GuiSliderC2S;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;

@Environment(EnvType.CLIENT)
public final class GuiNet {
    private GuiNet() {}

    public static void slider(int containerId, int slider, double value, boolean released) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new GuiSliderC2S(containerId, slider, value, released).write(buf);
        ClientPlayNetworking.send(GuiSliderC2S.CHANNEL, buf);
    }
}
