package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.WalkOnFluidPower;
import dev.overgrown.apoli.power.ApoliIds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class WalkOnFluidMixin {
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void apoli$canStandOnFluid(FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        boolean[] stand = new boolean[]{false};
        PowerLookup.forEach(self, ApoliIds.WALK_ON_FLUID, WalkOnFluidPower.Config.class, cfg -> {
            if (stand[0]) return;
            if (fluid.is(cfg.fluidTag())) stand[0] = true;
        });
        if (stand[0]) cir.setReturnValue(true);
    }
}
