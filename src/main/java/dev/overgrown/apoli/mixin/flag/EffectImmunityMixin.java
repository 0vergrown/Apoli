package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.power.builtin.EffectImmunityPower;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class EffectImmunityMixin {
    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void apoli$effectImmunity(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (EffectImmunityPower.isImmuneTo(self, effect)) {
            cir.setReturnValue(false);
        }
    }
}
