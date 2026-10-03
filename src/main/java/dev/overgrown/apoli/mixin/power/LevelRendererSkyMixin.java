package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.overgrown.apoli.power.builtin.ModifyFogHandler;
import dev.overgrown.apoli.power.builtin.ModifyFogInterpolator;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
public class LevelRendererSkyMixin {
    @WrapMethod(method = "renderSky")
    void apoli$preventRenderSky(Matrix4f matrix4f, Matrix4f matrix4f2, float f, Camera camera, boolean bl, Runnable runnable, Operation<Void> original) {
        ModifyFogHandler.FogData fog = ModifyFogInterpolator.getCurrent(null, null, null);
        if (!fog.color.equals(ModifyFogHandler.FogData.UNSET_COLOR)) return;

        original.call(matrix4f, matrix4f2, f, camera, bl, runnable);
    }
}
