package dev.overgrown.apoli.mixin.effects;

import dev.overgrown.apoli.effects.CustomEffectRegistry;
import dev.overgrown.apoli.effects.CustomMobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityReloadGateMixin {
    @Inject(method = "canBeAffected", at = @At(value = "HEAD"), cancellable = true)
    void apoli$preventEffectsDuringReload(MobEffectInstance mobEffectInstance, CallbackInfoReturnable<Boolean> cir) {
        if (!((LivingEntity) (Object) this).level().isClientSide() && (CustomEffectRegistry.reloading || !CustomEffectRegistry.waiting.isEmpty()) && mobEffectInstance.getEffect().value() instanceof CustomMobEffect) {
            cir.setReturnValue(false);
        }
    }
}
