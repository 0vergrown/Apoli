package dev.overgrown.apoli.mixin.hud;

import dev.overgrown.apoli.client.HudStatusStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsStatusRowMixin {

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At("HEAD"))
    private void apoli$noteBlit(ResourceLocation texture, int x1, int x2, int y1, int y2, int z,
                                float u1, float u2, float v1, float v2, CallbackInfo ci) {
        apoli$noteQuad(x1, y1, x2, y2);
    }

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At("HEAD"))
    private void apoli$noteTintedBlit(ResourceLocation texture, int x1, int x2, int y1, int y2, int z,
                                      float u1, float u2, float v1, float v2,
                                      float red, float green, float blue, float alpha, CallbackInfo ci) {
        apoli$noteQuad(x1, y1, x2, y2);
    }

    @Inject(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V", at = @At("HEAD"))
    private void apoli$noteFill(RenderType renderType, int x1, int y1, int x2, int y2, int z, int colour,
                                CallbackInfo ci) {
        apoli$noteQuad(x1, y1, x2, y2);
    }

    private void apoli$noteQuad(int x1, int y1, int x2, int y2) {
        if (!HudStatusStack.recording()) {
            return;
        }
        Matrix4f pose = ((GuiGraphics) (Object) this).pose().last().pose();
        int dx = (int) pose.m30();
        int dy = (int) pose.m31();
        HudStatusStack.noteDraw(x1 + dx, y1 + dy, x2 + dx, y2 + dy);
    }
}
