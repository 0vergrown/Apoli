package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.ApoliIds;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerElytraMixin {
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void apoli$elytraFlight(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self.onGround() || self.isFallFlying() || self.isInWater()) return;
        if (!PowerLookup.hasActive(self, ApoliIds.ELYTRA_FLIGHT)) return;
        self.startFallFlying();
        cir.setReturnValue(true);
    }
}
