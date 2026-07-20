package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.ApoliIds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityFlagMixin {
    @Inject(method = "onGround", at = @At("HEAD"), cancellable = true)
    private void apoli$grounded(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof LivingEntity living)) return;
        if (PowerLookup.hasActive(living, ApoliIds.GROUNDED)) {
            cir.setReturnValue(true);
        }
    }
}
