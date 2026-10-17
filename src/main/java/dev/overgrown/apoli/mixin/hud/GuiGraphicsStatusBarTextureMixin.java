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
    public abstract void blitSprite(ResourceLocation sprite, int x, int y, int blitOffset, int width, int height);

    @Shadow
    public abstract void blitSprite(ResourceLocation sprite, int textureWidth, int textureHeight, int u, int v,
                                    int x, int y, int blitOffset, int width, int height);

    @Shadow
    public abstract void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v,
                              int uWidth, int vHeight, int textureWidth, int textureHeight);

    @Inject(method = "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIII)V", at = @At("HEAD"), cancellable = true)
    private void apoli$replaceStatusBarSprite(ResourceLocation sprite, int x, int y, int blitOffset, int width, int height,
                                              CallbackInfo ci) {
        if (!StatusBarTextures.active()) return;
        ResourceLocation replacement = StatusBarTextures.sprite(sprite);
        if (replacement != null) {
            StatusBarTextures.beginReplacement();
            try {
                this.blitSprite(replacement, x, y, blitOffset, width, height);
            } finally {
                StatusBarTextures.endReplacement();
            }
            ci.cancel();
            return;
        }
        ResourceLocation sheet = StatusBarTextures.sheet();
        if (sheet == null) return;
        StatusBarSprites.Region region = StatusBarSprites.of(sprite);
        if (region == null || region.texture() != StatusBarSprites.ICONS) return;
        this.blit(sheet, x, y, width, height, region.u(), region.v(), region.width(), region.height(), 256, 256);
        ci.cancel();
    }

    @Inject(method = "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIIIIIII)V", at = @At("HEAD"), cancellable = true)
    private void apoli$replaceStatusBarSpriteRegion(ResourceLocation sprite, int textureWidth, int textureHeight, int u, int v,
                                                    int x, int y, int blitOffset, int width, int height, CallbackInfo ci) {
        if (!StatusBarTextures.active()) return;
        ResourceLocation replacement = StatusBarTextures.sprite(sprite);
        if (replacement != null) {
            StatusBarTextures.beginReplacement();
            try {
                this.blitSprite(replacement, textureWidth, textureHeight, u, v, x, y, blitOffset, width, height);
            } finally {
                StatusBarTextures.endReplacement();
            }
            ci.cancel();
            return;
        }
        ResourceLocation sheet = StatusBarTextures.sheet();
        if (sheet == null || textureWidth <= 0 || textureHeight <= 0) return;
        StatusBarSprites.Region region = StatusBarSprites.of(sprite);
        if (region == null || region.texture() != StatusBarSprites.ICONS) return;
        float scaleX = region.width() / (float) textureWidth;
        float scaleY = region.height() / (float) textureHeight;
        this.blit(sheet, x, y, width, height, region.u() + u * scaleX, region.v() + v * scaleY,
            Math.round(width * scaleX), Math.round(height * scaleY), 256, 256);
        ci.cancel();
    }
}
