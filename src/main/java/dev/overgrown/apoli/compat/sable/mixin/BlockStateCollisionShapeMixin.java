package dev.overgrown.apoli.compat.sable.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.compat.sable.SablePhasing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateCollisionShapeMixin {

    @ModifyReturnValue(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"))
    private VoxelShape apoli$phaseSubLevelBlocks(VoxelShape original, BlockGetter getter, BlockPos pos) {
        return SablePhasing.filter(original, getter, pos, (BlockState) (Object) this);
    }
}
