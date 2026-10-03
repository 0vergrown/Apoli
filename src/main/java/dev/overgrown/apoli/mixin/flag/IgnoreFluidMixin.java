package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.IgnoreFluidPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class IgnoreFluidMixin {
    @ModifyExpressionValue(method = "updateFluidHeightAndDoFluidPushing(Lnet/minecraft/tags/TagKey;D)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean apoli$ignoreFluidBlock(boolean original, @Local FluidState fluid,
                                           @Local BlockPos.MutableBlockPos pos) {
        if (!original) return false;
        Entity self = (Entity) (Object) this;
        if (!IgnoreFluidPower.mayIgnore(self)) return true;
        return !IgnoreFluidPower.ignores(self, fluid, pos);
    }
}
