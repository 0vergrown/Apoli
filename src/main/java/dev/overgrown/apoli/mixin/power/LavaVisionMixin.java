package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.LavaVisionHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
public abstract class LavaVisionMixin {

    @ModifyExpressionValue(method = "setupFog", at = @At(value = "CONSTANT", args = "floatValue=0.25F", ordinal = 0))
    private static float apoli$modifyLavaVisibilityMinWithoutFireResistance(float original, Camera camera) { //v
        return Math.max(0f, LavaVisionHandler.getModifier(camera.getEntity()).getB()) * 0.25f;
    }

    @ModifyExpressionValue(method = "setupFog", at = @At(value = "CONSTANT", args = "floatValue=1.0F", ordinal = 0))
    private static float apoli$modifyLavaVisibilityMaxWithoutFireResistance(float original, Camera camera) { //s
        return Math.max(0f, LavaVisionHandler.getModifier(camera.getEntity()).getA());
    }

    @ModifyExpressionValue(method = "setupFog", at = @At(value = "CONSTANT", args = "floatValue=0.0F", ordinal = 0))
    private static float apoli$modifyLavaVisibilityMinFireResistance(float original, Camera camera) { //v
        return Math.max(0f, LavaVisionHandler.getModifier(camera.getEntity()).getB());
    }

    @ModifyExpressionValue(method = "setupFog", at = @At(value = "CONSTANT", args = "floatValue=5.0F", ordinal = 0))
    private static float apoli$modifyLavaVisibilityMaxWithFireResistance(float original, Camera camera) { //s
        return Math.max(0f, LavaVisionHandler.getModifier(camera.getEntity()).getA());
    }
}

