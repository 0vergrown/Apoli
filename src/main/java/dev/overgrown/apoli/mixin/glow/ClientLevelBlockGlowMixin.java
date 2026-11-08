package dev.overgrown.apoli.mixin.glow;

import dev.overgrown.apoli.client.glow.BlockGlowScanner;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
@Environment(EnvType.CLIENT)
public abstract class ClientLevelBlockGlowMixin {

    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void apoli$glowBlockChanged(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        BlockGlowScanner.blockChanged(pos);
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"))
    private void apoli$glowChunkLoaded(ChunkPos pos, CallbackInfo ci) {
        BlockGlowScanner.chunkLoaded(pos.x, pos.z);
    }
}
