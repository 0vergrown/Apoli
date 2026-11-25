package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.overgrown.apoli.power.builtin.ModifyFogHandler;
import dev.overgrown.apoli.power.builtin.ModifyFogInterpolator;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class LevelRendererSkyMixin {
    @WrapWithCondition(method = "renderLevel",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V"))
    private boolean apoli$hideSkyBehindFogColor(LevelRenderer renderer, Matrix4f frustum, Matrix4f projection,
                                                float partialTick, Camera camera, boolean thickFog, Runnable fogSetup) {
        if (ModifyFogInterpolator.isIdle()) return true;
        return ModifyFogInterpolator.getCurrent(null, null, null).color.equals(ModifyFogHandler.FogData.UNSET_COLOR);
    }
}
