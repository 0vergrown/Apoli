package dev.overgrown.apoli.mixin.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsTooltipInvoker {
    @Invoker("renderTooltipInternal")
    void apoli$invokeRenderTooltipInternal(Font font, List<ClientTooltipComponent> components, int x, int y,
                                           ClientTooltipPositioner positioner);
}
