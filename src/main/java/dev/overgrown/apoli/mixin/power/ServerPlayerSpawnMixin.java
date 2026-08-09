package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyPlayerSpawnHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSpawnMixin {

    @ModifyReturnValue(method = "findRespawnPositionAndUseSpawnBlock", at = @At("RETURN"))
    private DimensionTransition apoli$modifyPlayerSpawn(DimensionTransition original) {
        return ModifyPlayerSpawnHandler.respawn((ServerPlayer) (Object) this, original);
    }
}
