package dev.overgrown.apoli.mixin.effects;

import dev.overgrown.apoli.effects.CustomEffectRegistry;
import dev.overgrown.apoli.effects.CustomMobEffect;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityCustomEffectMixin {
    @Inject(method = "onEffectRemoved", at = @At(value = "HEAD"))
    void apoli$onRemoveEffect(MobEffectInstance mobEffectInstance, CallbackInfo ci) {
        PowerContainer holder = PowerContainer.of((LivingEntity) (Object) this);
        if (holder != null) holder.removeAllFromSource(mobEffectInstance.getEffect().unwrapKey().get().location());
    }

    @Inject(method = "canBeAffected", at = @At(value = "HEAD"), cancellable = true)
    void apoli$preventEffectsDuringReload(MobEffectInstance effectInstance, CallbackInfoReturnable<Boolean> cir) {
        if ((CustomEffectRegistry.reloading || !CustomEffectRegistry.waiting.isEmpty()) && effectInstance.getEffect().value() instanceof CustomMobEffect) {
            cir.setReturnValue(false);
        }
    }
}
