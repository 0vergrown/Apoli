package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.ModifyPlayerSpawnHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerList.class)
public abstract class PlayerListSpawnMixin {

    @ModifyExpressionValue(method = "respawn", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerPlayer;getRespawnPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos apoli$modifyPlayerSpawn(BlockPos original, @Local(argsOnly = true) ServerPlayer player) {
        return ModifyPlayerSpawnHandler.respawnPosition(player, original);
    }
}
