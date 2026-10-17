package dev.overgrown.apoli.mixin.hud;

import dev.overgrown.apoli.client.StatusBarTextures;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiStatusBarTextureMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"))
    private void apoli$beginStatusBarTextures(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        StatusBarTextures.beginHud();
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("RETURN"))
    private void apoli$endStatusBarTextures(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        StatusBarTextures.endHud();
    }
}
