package dev.overgrown.apoli.mixin.damage;

import dev.overgrown.apoli.power.builtin.InvulnerabilityHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityInvulnerableMixin {
    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void apoli$invulnerability(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof LivingEntity self)) return;
        if (self.level().isClientSide()) return;
        if (InvulnerabilityHandler.isImmune(self, source, 0.0F, self.level())) {
            cir.setReturnValue(true);
        }
    }
}
