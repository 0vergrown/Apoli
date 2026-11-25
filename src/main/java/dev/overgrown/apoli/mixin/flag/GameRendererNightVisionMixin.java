package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.client.render.LocalNightVision;
import dev.overgrown.apoli.power.builtin.NightVisionPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class GameRendererNightVisionMixin {

    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void apoli$nightVisionPower(LivingEntity entity, float partialTick, CallbackInfoReturnable<Float> cir) {
        float power = LocalNightVision.strength(entity);
        if (power <= 0f) return;
        cir.setReturnValue(Math.max(power, NightVisionPower.effectScale(entity, partialTick)));
    }
}
