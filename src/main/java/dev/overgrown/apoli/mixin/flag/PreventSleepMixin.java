package dev.overgrown.apoli.mixin.flag;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import dev.overgrown.apoli.power.builtin.PreventSleepHandler;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class PreventSleepMixin {
    @Inject(method = "startSleepInBed(Lnet/minecraft/core/BlockPos;)Lcom/mojang/datafixers/util/Either;",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;FZZ)V"),
        cancellable = true)
    private void apoli$preventSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        PreventSleepHandler.Decision decision = PreventSleepHandler.decide(self, pos);
        if (decision == null) return;
        if (decision.setsSpawn() && EntitySleepEvents.ALLOW_SETTING_SPAWN.invoker().allowSettingSpawn(self, pos)) {
            self.setRespawnPosition(self.level().dimension(), pos, self.getYRot(), false, true);
        }
        self.displayClientMessage(decision.message(), true);
        cir.setReturnValue(Either.left(Player.BedSleepingProblem.OTHER_PROBLEM));
    }
}
