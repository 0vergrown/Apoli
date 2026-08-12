package dev.overgrown.apoli.compat.sable.mixin;

import dev.overgrown.apoli.compat.sable.SablePhasing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateCollisionShapeMixin {

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"), cancellable = true)
    private void apoli$phaseSubLevelBlocks(BlockGetter getter, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        VoxelShape filtered = SablePhasing.filter(cir.getReturnValue(), getter, pos, (BlockState) (Object) this);
        if (filtered != cir.getReturnValue()) cir.setReturnValue(filtered);
    }
}
