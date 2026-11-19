package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.access.KnownMovement;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerKnownMovementMixin implements KnownMovement {

    @Unique
    private double apoli$knownX;

    @Unique
    private double apoli$knownY;

    @Unique
    private double apoli$knownZ;

    @Unique
    private long apoli$knownMovementTime;

    @Inject(method = "checkMovementStatistics(DDD)V", at = @At("HEAD"))
    private void apoli$recordMovement(double dx, double dy, double dz, CallbackInfo ci) {
        apoli$knownX = dx;
        apoli$knownY = dy;
        apoli$knownZ = dz;
        apoli$knownMovementTime = ((Player) (Object) this).level().getGameTime();
    }

    @Override
    public double apoli$knownX() {
        return apoli$knownX;
    }

    @Override
    public double apoli$knownY() {
        return apoli$knownY;
    }

    @Override
    public double apoli$knownZ() {
        return apoli$knownZ;
    }

    @Override
    public long apoli$knownMovementTime() {
        return apoli$knownMovementTime;
    }
}
