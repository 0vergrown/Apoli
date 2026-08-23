package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.power.builtin.FluidVisionPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class FogRendererFluidVisionMixin {

    @Shadow private static float fogRed;
    @Shadow private static float fogGreen;
    @Shadow private static float fogBlue;

    @WrapOperation(method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogStart(F)V"))
    private static void apoli$fluidFogStart(float value, Operation<Void> original,
                                            Camera camera, FogRenderer.FogMode mode, float viewDistance,
                                            boolean thickFog, float partialTick) {
        FluidVisionPower.Config cfg = apoli$fluidVision(camera);
        original.call(cfg == null ? value : cfg.start());
    }

    @WrapOperation(method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogEnd(F)V"))
    private static void apoli$fluidFogEnd(float value, Operation<Void> original,
                                          Camera camera, FogRenderer.FogMode mode, float viewDistance,
                                          boolean thickFog, float partialTick) {
        FluidVisionPower.Config cfg = apoli$fluidVision(camera);
        original.call(cfg == null ? value : cfg.end());
    }

    @Inject(method = "setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V",
        at = @At("RETURN"))
    private static void apoli$fluidFogColor(Camera camera, float partialTick, ClientLevel level,
                                            int renderDistance, float darkness, CallbackInfo ci) {
        FluidVisionPower.Config cfg = apoli$fluidVision(camera);
        if (cfg == null || cfg.fogColor().isEmpty()) return;
        FluidVisionPower.FogColor color = cfg.fogColor().get();
        fogRed = color.red();
        fogGreen = color.green();
        fogBlue = color.blue();
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }

    private static FluidVisionPower.Config apoli$fluidVision(Camera camera) {
        return FluidVisionPower.activeFor(camera.getEntity(), camera.getFluidInCamera());
    }
}
