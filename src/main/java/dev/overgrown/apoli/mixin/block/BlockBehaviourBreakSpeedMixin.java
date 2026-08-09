package dev.overgrown.apoli.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyBreakSpeedHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourBreakSpeedMixin {

    private static final String GET_DESTROY_PROGRESS =
        "getDestroyProgress(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F";

    @ModifyExpressionValue(method = GET_DESTROY_PROGRESS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroySpeed"
            + "(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float apoli$modifyHardness(float original, BlockState state, Player player,
                                       BlockGetter level, BlockPos pos) {
        return ModifyBreakSpeedHandler.modifyHardness(original, player, level, pos, state);
    }

    @ModifyReturnValue(method = GET_DESTROY_PROGRESS, at = @At("RETURN"))
    private float apoli$modifyBreakSpeed(float original, BlockState state, Player player,
                                         BlockGetter level, BlockPos pos) {
        return ModifyBreakSpeedHandler.modifySpeed(original, player, level, pos, state);
    }
}
