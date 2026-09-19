package dev.overgrown.apoli.mixin.scale;

import dev.overgrown.apoli.power.builtin.SolidHitboxPower;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntitySolidHitboxMixin {

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void apoli$solidHitbox(CallbackInfoReturnable<Boolean> cir) {
        if (SolidHitboxPower.isSolid((Entity) (Object) this)) cir.setReturnValue(true);
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void apoli$solidNotPushable(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (SolidHitboxPower.isSolid(self) && !SolidHitboxPower.isPushable(self)) cir.setReturnValue(false);
    }

    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void apoli$solidOnlyForSome(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (SolidHitboxPower.isSolid(other) && !SolidHitboxPower.isSolidFor(other, (Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
