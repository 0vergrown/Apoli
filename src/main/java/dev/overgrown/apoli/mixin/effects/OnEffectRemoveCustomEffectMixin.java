package dev.overgrown.apoli.mixin.effects;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class OnEffectRemoveCustomEffectMixin {
    @Inject(method = "onEffectRemoved", at = @At(value = "HEAD"))
    void apoli$onRemoveEffect(MobEffectInstance mobEffectInstance, CallbackInfo ci) {
        PowerContainer holder = PowerContainer.of((LivingEntity) (Object) this);
        if (holder != null) holder.removeAllFromSource(mobEffectInstance.getEffect().unwrapKey().get().location());
    }
}
