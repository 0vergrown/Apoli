package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.EffectImmunityPower;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class EffectImmunityMixin {
    @ModifyReturnValue(method = "canBeAffected", at = @At("RETURN"))
    private boolean apoli$effectImmunity(boolean original, MobEffectInstance effect) {
        return original && !EffectImmunityPower.isImmuneTo((LivingEntity) (Object) this, effect);
    }
}
