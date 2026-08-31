package dev.overgrown.apoli.mixin.teleport;

import dev.overgrown.apoli.power.builtin.PreventTeleportHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Entity.class)
public abstract class EntityTeleportMixin {

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",
        at = @At("HEAD"), cancellable = true)
    private void apoli$preventTeleport(ServerLevel level, double x, double y, double z, Set<RelativeMovement> flags,
                                       float yaw, float pitch, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (PreventTeleportHandler.prevented(self, level != self.level())) cir.setReturnValue(false);
    }

    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"), cancellable = true)
    private void apoli$preventSimpleTeleport(double x, double y, double z, CallbackInfo ci) {
        if (PreventTeleportHandler.prevented((Entity) (Object) this, false)) ci.cancel();
    }

    @Inject(method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
        at = @At("HEAD"), cancellable = true)
    private void apoli$preventDimensionChange(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        Entity self = (Entity) (Object) this;
        if (PreventTeleportHandler.prevented(self, true)) cir.setReturnValue(self);
    }
}
