package dev.overgrown.apoli.mixin.input;

import dev.overgrown.apoli.client.ScrollWatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
@Environment(EnvType.CLIENT)
public abstract class MouseHandlerScrollMixin {
    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void apoli$scrollPower(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (yOffset == 0) return;
        if (window != Minecraft.getInstance().getWindow().getWindow()) return;
        if (ScrollWatcher.onScroll(yOffset)) ci.cancel();
    }
}
