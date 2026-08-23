package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.power.builtin.ModifyStatusEffectPower;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStatusEffectMixin {

    @ModifyVariable(
        method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance apoli$modifyStatusEffect(MobEffectInstance original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return original;
        return ModifyStatusEffectPower.apply(self, original);
    }
}
