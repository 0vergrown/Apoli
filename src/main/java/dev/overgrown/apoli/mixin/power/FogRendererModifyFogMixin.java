package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.power.builtin.ModifyFogHandler;
import dev.overgrown.apoli.power.builtin.ModifyFogInterpolator;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FogRenderer.class)
public class FogRendererModifyFogMixin {
    @Shadow  private static float fogRed;
    @Shadow private static float fogGreen;
    @Shadow private static float fogBlue;

    @Inject(
            method = "setupColor",
            at = @At(value = "RETURN")
    )
    private static void apoli$modifyFogColor(Camera camera, float f, ClientLevel clientLevel, int i, float g, CallbackInfo ci) {
        var base_color = new Vec3(fogRed, fogGreen, fogBlue);
        ModifyFogHandler.FogData fog = ModifyFogInterpolator.getCurrent(null, null, base_color);
        ModifyFogInterpolator.setDefaultColor(base_color);
        if (!fog.color.equals(ModifyFogHandler.FogData.EMPTY.color)) {
            fogRed = (float) fog.color.x;
            fogGreen = (float) fog.color.y;
            fogBlue = (float) fog.color.z;

            RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 1f);
        }
    }

    @WrapOperation(
            method = "setupFog",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogStart (F)V")
    )
    private static void apoli$modifyFogNear(float f, Operation<Void> original) {
        ModifyFogHandler.FogData fog = ModifyFogInterpolator.getCurrent(f, null, null);
        ModifyFogInterpolator.setDefaultS(f);
        if (fog.s == ModifyFogHandler.FogData.EMPTY.s) {
            original.call(f);
        }
        else {
            original.call(fog.s);
        }
    }
    @WrapOperation(
            method = "setupFog",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogEnd (F)V")
    )
    private static void apoli$modifyFogFar(float f, Operation<Void> original) {
        ModifyFogHandler.FogData fog = ModifyFogInterpolator.getCurrent(null, f, null);
        ModifyFogInterpolator.setDefaultV(f);
        if (fog.v == ModifyFogHandler.FogData.EMPTY.v) {
            original.call(f);
        }
        else {
            original.call(fog.v);
        }
    }
}
