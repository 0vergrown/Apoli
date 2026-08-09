package dev.overgrown.apoli.mixin.block;

import dev.overgrown.apoli.power.builtin.BlockBreakHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeBlockBreakMixin {

    @Shadow @Final protected ServerPlayer player;

    @Shadow protected ServerLevel level;

    @Unique private BlockState apoli$brokenState;

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"))
    private void apoli$captureBrokenState(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        this.apoli$brokenState = this.level.getBlockState(pos);
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"))
    private void apoli$actionOnBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = this.apoli$brokenState;
        this.apoli$brokenState = null;
        if (state == null || !cir.getReturnValueZ()) return;
        BlockBreakHandler.fire(this.player, this.level, pos, state,
            this.player.hasCorrectToolForDrops(state));
    }
}
