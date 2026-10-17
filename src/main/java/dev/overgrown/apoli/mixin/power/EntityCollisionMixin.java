package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.PreventEntityCollisionPower;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityCollisionMixin {

    @ModifyExpressionValue(method = "push(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isPassengerOfSameVehicle(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean apoli$preventEntityPush(boolean original, Entity other) {
        return original || PreventEntityCollisionPower.prevents((Entity) (Object) this, other);
    }

    @ModifyReturnValue(method = "canCollideWith(Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private boolean apoli$preventHardCollision(boolean original, Entity other) {
        return original && !PreventEntityCollisionPower.prevents((Entity) (Object) this, other);
    }
}
