package dev.overgrown.apoli.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.BlockRenderHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseSelectionMixin {

    @ModifyReturnValue(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
        + "Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"))
    private VoxelShape apoli$preventBlockSelection(VoxelShape original, BlockGetter level, BlockPos pos,
                                                   CollisionContext context) {
        if (original.isEmpty()) return original;
        if (!(context instanceof EntityCollisionContext entityContext)) return original;
        if (!(entityContext.getEntity() instanceof Player player)) return original;
        if (BlockRenderHandler.hasNone(player, dev.overgrown.apoli.power.ApoliIds.PREVENT_BLOCK_SELECTION)) {
            return original;
        }
        if (!(level instanceof Level realLevel)) return original;
        return BlockRenderHandler.preventsSelection(player, realLevel, pos) ? Shapes.empty() : original;
    }
}
