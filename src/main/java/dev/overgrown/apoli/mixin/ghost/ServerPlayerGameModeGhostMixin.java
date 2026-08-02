package dev.overgrown.apoli.mixin.ghost;

import dev.overgrown.apoli.block.GhostBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeGhostMixin {

    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void apoli$refuseGhostBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (GhostBlocks.isGhost(this.level, pos)) {
            this.player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(
                this.level, pos));
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void apoli$refuseGhostUse(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                      BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (GhostBlocks.isGhost(level, hitResult.getBlockPos())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
