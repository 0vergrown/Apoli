package dev.overgrown.apoli.client.gui;

import dev.overgrown.apoli.network.payload.GuiSliderC2S;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public final class GuiNet {
    private GuiNet() {}

    public static void slider(int containerId, int slider, double value, boolean released) {
        PacketDistributor.sendToServer(new GuiSliderC2S(containerId, slider, value, released));
    }
}
