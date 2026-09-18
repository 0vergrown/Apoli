package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.PreventEntityCollisionPower;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCollisionMixin {

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void apoli$preventEntityPush(Entity other, CallbackInfo ci) {
        if (PreventEntityCollisionPower.prevents((Entity) (Object) this, other)) {
            ci.cancel();
        }
    }

    @Inject(method = "canCollideWith(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void apoli$preventHardCollision(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (PreventEntityCollisionPower.prevents((Entity) (Object) this, other)) {
            cir.setReturnValue(false);
        }
    }
}
