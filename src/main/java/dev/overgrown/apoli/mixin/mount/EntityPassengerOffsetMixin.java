package dev.overgrown.apoli.mixin.mount;

import dev.overgrown.apoli.mount.MountOffsets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPassengerOffsetMixin {

    @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void apoli$mountOffset(Entity passenger, CallbackInfo ci) {
        if (MountOffsets.get(passenger.getId()) == null) return;
        Entity self = (Entity) (Object) this;
        if (!self.hasPassenger(passenger)) return;
        Vec3 delta = MountOffsets.resolve(self, passenger);
        if (delta.x == 0.0 && delta.y == 0.0 && delta.z == 0.0) return;
        passenger.setPos(passenger.getX() + delta.x, passenger.getY() + delta.y, passenger.getZ() + delta.z);
    }

    @Inject(method = "removeVehicle", at = @At("HEAD"))
    private void apoli$clearMountOffset(CallbackInfo ci) {
        MountOffsets.clear(((Entity) (Object) this).getId());
    }
}
