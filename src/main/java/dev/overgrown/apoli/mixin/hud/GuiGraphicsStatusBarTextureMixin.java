package dev.overgrown.apoli.mixin.hud;

import dev.overgrown.apoli.client.StatusBarSprites;
import dev.overgrown.apoli.client.StatusBarTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsStatusBarTextureMixin {

    @Shadow
    public abstract void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v,
                              int uWidth, int vHeight, int textureWidth, int textureHeight);

    @Inject(method = "blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", at = @At("HEAD"), cancellable = true)
    private void apoli$replaceStatusBarIcon(ResourceLocation texture, int x, int y, int u, int v, int width, int height,
                                            CallbackInfo ci) {
        if (!StatusBarTextures.active()) return;
        StatusBarSprites.Region region = StatusBarSprites.at(texture, u, v);
        if (region == null) return;
        int du = u - region.u();
        int dv = v - region.v();
        ResourceLocation replacement = StatusBarTextures.sprite(region.sprite());
        if (replacement != null) {
            StatusBarSprites.Region vanilla = StatusBarSprites.of(replacement);
            if (vanilla != null) {
                float scaleX = vanilla.width() / (float) region.width();
                float scaleY = vanilla.height() / (float) region.height();
                this.blit(vanilla.texture(), x, y, width, height, vanilla.u() + du * scaleX, vanilla.v() + dv * scaleY,
                    Math.round(width * scaleX), Math.round(height * scaleY), 256, 256);
            } else {
                this.blit(StatusBarSprites.spriteTexture(replacement), x, y, width, height, du, dv, width, height,
                    region.width(), region.height());
            }
            ci.cancel();
            return;
        }
        ResourceLocation sheet = StatusBarTextures.sheet();
        if (sheet == null || region.texture() != StatusBarSprites.ICONS) return;
        this.blit(sheet, x, y, width, height, u, v, width, height, 256, 256);
        ci.cancel();
    }
}
