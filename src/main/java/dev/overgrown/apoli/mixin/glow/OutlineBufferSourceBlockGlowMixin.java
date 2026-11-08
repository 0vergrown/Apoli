package dev.overgrown.apoli.mixin.glow;

import dev.overgrown.apoli.client.glow.BlockGlowRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.OutlineBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OutlineBufferSource.class)
@Environment(EnvType.CLIENT)
public abstract class OutlineBufferSourceBlockGlowMixin {

    @Inject(method = "setColor", at = @At("HEAD"))
    private void apoli$noteEntityOutline(int red, int green, int blue, int alpha, CallbackInfo ci) {
        BlockGlowRenderer.noteEntityOutline();
    }
}
