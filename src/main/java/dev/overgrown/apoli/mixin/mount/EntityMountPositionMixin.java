package dev.overgrown.apoli.mixin.mount;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMountPositionMixin {

    @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V",
            at = @At("HEAD"), cancellable = true)
    private void apoli$rideOnHead(Entity passenger, Entity.MoveFunction callback, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;
        callback.accept(passenger, player.getX(),
            player.getY() + player.getBbHeight() + passenger.getMyRidingOffset(), player.getZ());
        ci.cancel();
    }
}
