package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.access.KnownMovement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerKnownMovementMixin implements KnownMovement {

    @Unique
    private long apoli$knownMovementTime;

    @Inject(method = "setKnownMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void apoli$stampMovement(Vec3 movement, CallbackInfo ci) {
        apoli$knownMovementTime = ((ServerPlayer) (Object) this).level().getGameTime();
    }

    @Override
    public long apoli$knownMovementTime() {
        return apoli$knownMovementTime;
    }
}
