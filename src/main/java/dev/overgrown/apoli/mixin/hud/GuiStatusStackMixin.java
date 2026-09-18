package dev.overgrown.apoli.mixin.hud;

import dev.overgrown.apoli.client.HudStatusStack;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiStatusStackMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At("HEAD"))
    private void apoli$beginStatusStack(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        HudStatusStack.beginFrame(graphics.guiWidth(), graphics.guiHeight());
    }
}
