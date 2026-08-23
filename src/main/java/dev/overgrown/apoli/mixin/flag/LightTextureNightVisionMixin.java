package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.power.builtin.NightVisionPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
@OnlyIn(Dist.CLIENT)
public abstract class LightTextureNightVisionMixin {

    @ModifyExpressionValue(method = "updateLightTexture",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z",
            ordinal = 0))
    private boolean apoli$forceNightVision(boolean original) {
        return original || NightVisionPower.strengthFor(Minecraft.getInstance().player) > 0f;
    }

    @WrapOperation(method = "updateLightTexture",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private float apoli$nightVisionScale(LivingEntity entity, float partialTick, Operation<Float> original) {
        float power = NightVisionPower.strengthFor(entity);
        Holder<MobEffect> nightVision = MobEffects.NIGHT_VISION;
        if (!entity.hasEffect(nightVision)) return power;
        return Math.max(original.call(entity, partialTick), power);
    }
}
