package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyPlayerSpawnHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSpawnMixin {

    @ModifyReturnValue(method = "getRespawnPosition", at = @At("RETURN"))
    private BlockPos apoli$modifyPlayerSpawn(BlockPos original) {
        return ModifyPlayerSpawnHandler.respawnPosition((ServerPlayer) (Object) this, original);
    }
}
