package dev.overgrown.apoli.mixin.scale;

import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityScaleMixin {

    @Inject(method = "getSpeed()F", at = @At("RETURN"), cancellable = true)
    private void apoli$scaleSpeed(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Scales.untouched(self)) return;
        float motion = Scales.applied(self, ScaleTypes.MOTION);
        if (motion != 1.0F) cir.setReturnValue(cir.getReturnValueF() * motion);
    }

    @Inject(method = "getJumpPower()F", at = @At("RETURN"), cancellable = true)
    private void apoli$scaleJump(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Scales.untouched(self)) return;
        float jump = Scales.applied(self, ScaleTypes.JUMP_HEIGHT);
        if (jump != 1.0F) cir.setReturnValue(cir.getReturnValueF() * jump);
    }

    @ModifyVariable(method = "causeFallDamage(FFLnet/minecraft/world/damagesource/DamageSource;)Z",
                    at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float apoli$scaleFallDistance(float fallDistance) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Scales.untouched(self)) return fallDistance;
        float falling = Scales.applied(self, ScaleTypes.FALLING);
        return falling == 1.0F || falling <= 0.0F ? fallDistance : fallDistance / falling;
    }

    @ModifyVariable(method = "knockback(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double apoli$scaleKnockback(double strength) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Scales.untouched(self)) return strength;
        float knockback = Scales.applied(self, ScaleTypes.KNOCKBACK);
        return knockback == 1.0F || knockback <= 0.0F ? strength : strength / knockback;
    }

    @Inject(method = "maxUpStep()F", at = @At("RETURN"), cancellable = true)
    private void apoli$scaleStepHeight(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Scales.untouched(self)) return;
        float step = Scales.applied(self, ScaleTypes.STEP_HEIGHT);
        if (step != 1.0F) cir.setReturnValue(cir.getReturnValueF() * step);
    }

    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void apoli$scaleVisibility(Entity looker, CallbackInfoReturnable<Double> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Scales.untouched(self)) return;
        float visibility = Scales.applied(self, ScaleTypes.VISIBILITY);
        if (visibility != 1.0F) cir.setReturnValue(cir.getReturnValueD() * visibility);
    }
}
