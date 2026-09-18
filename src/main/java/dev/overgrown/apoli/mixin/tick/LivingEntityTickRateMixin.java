package dev.overgrown.apoli.mixin.tick;

import dev.overgrown.apoli.client.ClientTickRates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
@Environment(EnvType.CLIENT)
public abstract class LivingEntityTickRateMixin {

    @ModifyVariable(method = "lerpHeadTo(FI)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int apoli$headLerpSteps(int steps) {
        Entity self = (Entity) (Object) this;
        if (ClientTickRates.idle() || !self.level().isClientSide()) return steps;
        return ClientTickRates.headLerpSteps(self, steps);
    }

    @ModifyArg(
        method = "calculateEntityAnimation(Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;updateWalkAnimation(F)V"))
    private float apoli$slowedWalkAnimation(float distance) {
        Entity self = (Entity) (Object) this;
        if (ClientTickRates.idle() || !self.level().isClientSide()) return distance;
        return ClientTickRates.walkAnimationInput(self, distance);
    }
}
