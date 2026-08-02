package dev.overgrown.apoli.mixin.ghost;

import dev.overgrown.apoli.block.GhostBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockGhostMixin {

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void apoli$ghostBlocksAreImmovable(BlockState state, Level level, BlockPos pos, Direction moveDirection,
                                                      boolean allowDestroy, Direction pistonFacing,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (GhostBlocks.isGhost(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
