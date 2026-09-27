package dev.overgrown.apoli.client;

import dev.overgrown.apoli.mixin.hud.GuiGraphicsTooltipInvoker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class TooltipRenderer {
    private TooltipRenderer() {}

    public static void render(GuiGraphics graphics, Font font, List<ClientTooltipComponent> components,
                              int mouseX, int mouseY) {
        if (components.isEmpty()) return;
        ((GuiGraphicsTooltipInvoker) graphics).apoli$invokeRenderTooltipInternal(
            font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE);
    }
}
