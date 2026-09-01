package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.IgnoreFluidPower;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class IgnoreFluidMixin {
    @Inject(method = "updateFluidHeightAndDoFluidPushing(Lnet/minecraft/tags/TagKey;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void apoli$ignoreFluid(TagKey<Fluid> fluidTag, double motionScale, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        PowerContainer container = PowerContainer.of(self);
        if (container == null || container.isEmpty()) return;
        if (container.powersOfType(ApoliIds.IGNORE_FLUID).isEmpty()) return;

        BlockPos pos = self.blockPosition();
        FluidState fluid = self.level().getFluidState(pos);
        if (fluid.isEmpty() || !fluid.is(fluidTag)) return;
        if (IgnoreFluidPower.ignores(self, fluid, pos)) cir.setReturnValue(false);
    }
}
