package dev.overgrown.apoli.compat.sable.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockCollisionShapeMixin {

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;"
        + "Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
        + "Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"), cancellable = true)
    private void apoli$restoreFluidWalking(BlockState state, BlockGetter getter, BlockPos pos,
                                           CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!cir.getReturnValue().isEmpty()) return;
        if (state.getValue(LiquidBlock.LEVEL) != 0) return;
        if (!(context instanceof EntityCollisionContext entityContext)) return;
        if (!(entityContext.getEntity() instanceof LivingEntity living)) return;
        if (!context.isAbove(LiquidBlock.STABLE_SHAPE, pos, true)) return;
        FluidState own = state.getFluidState();
        if (!living.canStandOnFluid(own)) return;
        if (getter.getFluidState(pos.above()).getType().isSame(own.getType())) return;
        cir.setReturnValue(LiquidBlock.STABLE_SHAPE);
    }
}
