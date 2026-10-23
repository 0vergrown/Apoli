package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ShareEffectsPower;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityShareEffectsMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("RETURN"))
    private void apoli$shareEffects(MobEffectInstance instance, @Nullable Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) ShareEffectsPower.onEffectAdded((LivingEntity) (Object) this, instance);
    }
}
