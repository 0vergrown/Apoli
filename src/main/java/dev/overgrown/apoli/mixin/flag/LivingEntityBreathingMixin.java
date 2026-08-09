package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.WaterBreathingPower;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBreathingMixin {

    @ModifyReturnValue(method = "canBreatheUnderwater()Z", at = @At("RETURN"))
    private boolean apoli$canBreatheUnderwater(boolean original) {
        return original || WaterBreathingPower.canBreatheUnderwater((LivingEntity) (Object) this);
    }

    @Inject(method = "baseTick()V", at = @At("TAIL"))
    private void apoli$waterBreathingTick(CallbackInfo ci) {
        WaterBreathingPower.tick((LivingEntity) (Object) this);
    }
}
