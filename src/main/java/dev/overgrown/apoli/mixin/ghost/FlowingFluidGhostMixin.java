package dev.overgrown.apoli.mixin.ghost;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.block.GhostBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidGhostMixin {

    @ModifyReturnValue(method = "canHoldFluid", at = @At("RETURN"))
    private boolean apoli$ghostBlocksHoldNoFluid(boolean original, BlockGetter level, BlockPos pos, BlockState state,
                                                 Fluid fluid) {
        return original && !(level instanceof Level world && GhostBlocks.isGhost(world, pos));
    }
}
