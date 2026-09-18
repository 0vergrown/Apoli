package dev.overgrown.apoli.mixin.hud;

import dev.overgrown.apoli.client.HudStatusStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenStatusStackMixin {

    @Inject(method = "renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"))
    private void apoli$endStatusStack(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        HudStatusStack.endFrame();
    }
}
