package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.power.builtin.NightVisionPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
@Environment(EnvType.CLIENT)
public abstract class LightTextureNightVisionMixin {

    @ModifyExpressionValue(method = "updateLightTexture(F)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z",
            ordinal = 0))
    private boolean apoli$forceNightVision(boolean original) {
        return original || NightVisionPower.strengthFor(Minecraft.getInstance().player) > 0f;
    }

    @WrapOperation(method = "updateLightTexture(F)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private float apoli$nightVisionScale(LivingEntity entity, float partialTick, Operation<Float> original) {
        float power = NightVisionPower.strengthFor(entity);
        if (!entity.hasEffect(MobEffects.NIGHT_VISION)) return power;
        return Math.max(original.call(entity, partialTick), power);
    }
}
