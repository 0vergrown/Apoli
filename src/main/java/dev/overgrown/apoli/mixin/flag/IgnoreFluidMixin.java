package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.IgnoreFluidPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class IgnoreFluidMixin {
    @ModifyExpressionValue(method = "updateFluidHeightAndDoFluidPushing()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getFluidType()Lnet/neoforged/neoforge/fluids/FluidType;"))
    private FluidType apoli$ignoreFluidBlock(FluidType original, @Local FluidState fluid,
                                             @Local BlockPos.MutableBlockPos pos) {
        if (original.isAir()) return original;
        Entity self = (Entity) (Object) this;
        if (!IgnoreFluidPower.mayIgnore(self)) return original;
        return IgnoreFluidPower.ignores(self, fluid, pos) ? NeoForgeMod.EMPTY_TYPE.value() : original;
    }

    @ModifyExpressionValue(method = "updateSwimming",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;canStartSwimming()Z"))
    private boolean apoli$noSwimmingInIgnoredFluid(boolean original) {
        if (!original) return false;
        Entity self = (Entity) (Object) this;
        return self.isInFluidType() || !IgnoreFluidPower.holds(self);
    }
}
