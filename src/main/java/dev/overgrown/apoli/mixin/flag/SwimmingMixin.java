package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import dev.overgrown.apoli.power.builtin.SwimmingPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class SwimmingMixin {

    @Shadow protected boolean wasTouchingWater;

    @Unique
    private boolean apoli$powerSwimming;

    @Inject(method = "updateSwimming", at = @At("HEAD"))
    private void apoli$swimming(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        this.apoli$powerSwimming = self instanceof LivingEntity living && SwimmingPower.holds(living);
        if (!this.apoli$powerSwimming) return;
        boolean swimming = self.isSprinting() && !self.isPassenger();
        boolean submerged = self.isInWater();
        self.setSwimming(swimming);
        if (swimming && !submerged) {
            this.wasTouchingWater = true;
            self.fallDistance = 0.0F;
            self.move(MoverType.SELF, self.getLookAngle().scale(0.25));
        }
    }

    @WrapWithCondition(method = "updateSwimming",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSwimming(Z)V"))
    private boolean apoli$keepPowerSwimming(Entity self, boolean swimming) {
        return !this.apoli$powerSwimming;
    }
}
