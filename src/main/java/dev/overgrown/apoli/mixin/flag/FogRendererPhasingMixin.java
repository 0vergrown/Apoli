package dev.overgrown.apoli.mixin.flag;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.client.PhasingBlindness;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FogRenderer.class, priority = 1500)
@OnlyIn(Dist.CLIENT)
public abstract class FogRendererPhasingMixin {

    @Shadow private static float fogRed;
    @Shadow private static float fogGreen;
    @Shadow private static float fogBlue;

    @Inject(method = "setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V",
        at = @At("RETURN"))
    private static void apoli$phasingBlindnessColor(Camera camera, float partialTick, ClientLevel level,
                                                     int renderDistance, float darkness, CallbackInfo ci) {
        Entity focus = camera.getEntity();
        if (PhasingBlindness.viewDistance(focus) < 0.0F) return;
        if (!PhasingBlindness.inWall(focus)) return;
        fogRed = 0.0F;
        fogGreen = 0.0F;
        fogBlue = 0.0F;
        RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
    }

    @Inject(method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V",
        at = @At("RETURN"))
    private static void apoli$phasingBlindnessFog(Camera camera, FogRenderer.FogMode fogMode, float f, boolean bl,
                                                   float g, CallbackInfo ci) {
        Entity focus = camera.getEntity();
        float view = PhasingBlindness.viewDistance(focus);
        if (view < 0.0F) return;
        if (!PhasingBlindness.inWall(focus)) return;
        if (fogMode == FogRenderer.FogMode.FOG_SKY) {
            RenderSystem.setShaderFogStart(0.0F);
            RenderSystem.setShaderFogEnd(view * 0.8F);
        } else {
            RenderSystem.setShaderFogStart(view * 0.25F);
            RenderSystem.setShaderFogEnd(view);
        }
    }
}
