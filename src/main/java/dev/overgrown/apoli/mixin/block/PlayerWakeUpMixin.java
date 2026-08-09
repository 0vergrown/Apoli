package dev.overgrown.apoli.mixin.block;

import dev.overgrown.apoli.power.builtin.WakeUpHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Player.class)
public abstract class PlayerWakeUpMixin {

    @Inject(method = "stopSleepInBed(ZZ)V", at = @At("HEAD"))
    private void apoli$actionOnWakeUp(boolean wakeImmediately, boolean updateLevelForSleepingPlayers,
                                      CallbackInfo ci) {
        Player self = (Player) (Object) this;
        Optional<BlockPos> bed = self.getSleepingPos();
        if (bed.isEmpty()) return;
        WakeUpHandler.fire(self, bed.get());
    }
}
