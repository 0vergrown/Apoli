package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class SwimmingMixin {
    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void apoli$swimming(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LivingEntity living)) return;
        if (!PowerLookup.hasActive(living, Apoli.id("swimming"))) return;
        if (!self.isSprinting() || self.isPassenger()) {
            self.setSwimming(false);
            ci.cancel();
            return;
        }
        self.setSwimming(true);
        ci.cancel();
    }
}
