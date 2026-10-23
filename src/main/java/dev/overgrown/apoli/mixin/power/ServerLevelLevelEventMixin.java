package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.PreventLevelEventPower;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelLevelEventMixin {

    @Inject(method = "levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V",
        at = @At("HEAD"), cancellable = true)
    private void apoli$preventLevelEvent(@Nullable Player player, int event, BlockPos pos, int data, CallbackInfo ci) {
        if (PreventLevelEventPower.prevents(player, event)) ci.cancel();
    }
}
