package dev.overgrown.apoli.mixin.rope;

import dev.overgrown.apoli.rope.RopeManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerRopeElytraMixin {

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void apoli$blockRopeElytra(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (RopeManager.controllableRopeOf(self.getUUID()) != null) {
            cir.setReturnValue(false);
        }
    }
}
