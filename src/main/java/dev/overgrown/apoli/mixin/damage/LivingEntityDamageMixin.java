package dev.overgrown.apoli.mixin.damage;

import dev.overgrown.apoli.power.builtin.HitActionHandler;
import dev.overgrown.apoli.power.builtin.ModifyDamageHandler;
import dev.overgrown.apoli.power.builtin.ModifyProjectileDamageHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @Unique private float apoli$modifiedDamage;

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"), cancellable = true)
    private void apoli$modifyDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            apoli$modifiedDamage = amount;
            return;
        }
        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : null;
        float afterProjectile = ModifyProjectileDamageHandler.modifyAmount(attacker, self, source, amount, self.level());
        apoli$modifiedDamage = ModifyDamageHandler.modifyAmount(attacker, self, source, afterProjectile, self.level());
        if (amount > 0 && apoli$modifiedDamage <= 0) cir.setReturnValue(false);
    }

    @ModifyVariable(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float apoli$applyModifiedDamage(float original) {
        return apoli$modifiedDamage;
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void apoli$invokeHitActions(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : null;
        HitActionHandler.fire(attacker, self, source, amount, self.level(), Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
