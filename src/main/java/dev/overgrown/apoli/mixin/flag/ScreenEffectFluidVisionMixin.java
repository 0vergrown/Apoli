package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.power.builtin.FluidVisionPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class ScreenEffectFluidVisionMixin {

    @Inject(method = "renderWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At("HEAD"), cancellable = true)
    private static void apoli$suppressWaterOverlay(Minecraft minecraft, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                   CallbackInfo ci) {
        Entity camera = minecraft.getCameraEntity();
        FluidVisionPower.Config cfg = FluidVisionPower.activeFor(camera, FogType.WATER);
        if (cfg != null && !cfg.renderOverlay()) ci.cancel();
    }
}
