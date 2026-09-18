package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class SwimmingMixin {

    @Shadow protected boolean wasTouchingWater;

    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void apoli$swimming(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LivingEntity living)) return;
        if (!PowerLookup.hasActive(living, ApoliIds.SWIMMING)) return;
        boolean swimming = self.isSprinting() && !self.isPassenger();
        boolean submerged = self.isInWater();
        self.setSwimming(swimming);
        if (swimming && !submerged) {
            this.wasTouchingWater = true;
            self.fallDistance = 0.0F;
            self.move(MoverType.SELF, self.getLookAngle().scale(0.25));
        }
        ci.cancel();
    }
}
