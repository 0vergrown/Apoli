package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.WalkOnFluidPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class WalkOnFluidMixin {
    @ModifyReturnValue(method = "canStandOnFluid", at = @At("RETURN"))
    private boolean apoli$canStandOnFluid(boolean original, FluidState fluid) {
        if (original || fluid.isEmpty()) return original;
        return WalkOnFluidPower.standsOn((LivingEntity) (Object) this, fluid);
    }
}
