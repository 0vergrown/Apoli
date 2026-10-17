package dev.overgrown.apoli.mixin.scale;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityScaleMixin {

    @WrapMethod(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F")
    private float apoli$scaleEyeHeight(Pose pose, EntityDimensions dimensions, Operation<Float> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Scales.untouched(self)) return original.call(pose, dimensions);
        float width = Scales.applied(self, ScaleTypes.HITBOX_WIDTH);
        float height = Scales.applied(self, ScaleTypes.HITBOX_HEIGHT);
        float eye = Scales.applied(self, ScaleTypes.EYE_HEIGHT);
        if (width == 1.0F && height == 1.0F && eye == 1.0F) return original.call(pose, dimensions);
        if (dimensions.fixed) {
            float raw = original.call(pose, dimensions);
            return eye == height ? raw : raw * eye;
        }
        EntityDimensions unscaled = width > 0.0F && height > 0.0F ? dimensions.scale(1.0F / width, 1.0F / height) : dimensions;
        return original.call(pose, unscaled) * eye;
    }

    @ModifyReturnValue(method = "getSpeed()F", at = @At("RETURN"))
    private float apoli$scaleSpeed(float original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Scales.untouched(self)) return original;
        float motion = Scales.applied(self, ScaleTypes.MOTION);
        return motion != 1.0F ? original * motion : original;
    }

    @ModifyReturnValue(method = "getJumpPower()F", at = @At("RETURN"))
    private float apoli$scaleJump(float original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Scales.untouched(self)) return original;
        float jump = Scales.applied(self, ScaleTypes.JUMP_HEIGHT);
        return jump != 1.0F ? original * jump : original;
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

    @ModifyReturnValue(method = "maxUpStep()F", at = @At("RETURN"))
    private float apoli$scaleStepHeight(float original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Scales.untouched(self)) return original;
        float step = Scales.applied(self, ScaleTypes.STEP_HEIGHT);
        return step != 1.0F ? original * step : original;
    }

    @ModifyReturnValue(method = "getVisibilityPercent", at = @At("RETURN"))
    private double apoli$scaleVisibility(double original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Scales.untouched(self)) return original;
        float visibility = Scales.applied(self, ScaleTypes.VISIBILITY);
        return visibility != 1.0F ? original * visibility : original;
    }
}
