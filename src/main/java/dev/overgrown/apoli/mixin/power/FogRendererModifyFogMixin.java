package dev.overgrown.apoli.mixin.power;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.power.builtin.ModifyFogHandler;
import dev.overgrown.apoli.power.builtin.ModifyFogInterpolator;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FogRenderer.class)
public class FogRendererModifyFogMixin {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;

    @Inject(method = "setupColor",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clearColor(FFFF)V", shift = At.Shift.AFTER))
    private static void apoli$modifyFogColor(CallbackInfo ci) {
        if (ModifyFogInterpolator.isIdle()) return;
        Vec3 vanilla = new Vec3(fogRed, fogGreen, fogBlue);
        ModifyFogInterpolator.setDefaultColor(vanilla);
        ModifyFogHandler.FogData fog = ModifyFogInterpolator.getCurrent(null, null, vanilla);
        if (fog.color.equals(ModifyFogHandler.FogData.UNSET_COLOR)) return;
        fogRed = (float) fog.color.x;
        fogGreen = (float) fog.color.y;
        fogBlue = (float) fog.color.z;
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0f);
    }

    @ModifyArg(method = "setupFog",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogStart(F)V"))
    private static float apoli$modifyFogNear(float vanilla) {
        if (ModifyFogInterpolator.isIdle()) return vanilla;
        ModifyFogInterpolator.setDefaultS(vanilla);
        ModifyFogHandler.FogData fog = ModifyFogInterpolator.getCurrent(vanilla, null, null);
        return fog.s == ModifyFogHandler.FogData.UNSET ? vanilla : fog.s;
    }

    @ModifyArg(method = "setupFog",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogEnd(F)V"))
    private static float apoli$modifyFogFar(float vanilla) {
        if (ModifyFogInterpolator.isIdle()) return vanilla;
        ModifyFogInterpolator.setDefaultV(vanilla);
        ModifyFogHandler.FogData fog = ModifyFogInterpolator.getCurrent(null, vanilla, null);
        return fog.v == ModifyFogHandler.FogData.UNSET ? vanilla : fog.v;
    }
}
