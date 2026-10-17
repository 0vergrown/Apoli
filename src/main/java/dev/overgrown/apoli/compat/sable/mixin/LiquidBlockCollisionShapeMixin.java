package dev.overgrown.apoli.compat.sable.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockCollisionShapeMixin {

    @ModifyReturnValue(method = "getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;"
        + "Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
        + "Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"))
    private VoxelShape apoli$restoreFluidWalking(VoxelShape original, BlockState state, BlockGetter getter,
                                                BlockPos pos, CollisionContext context) {
        if (!original.isEmpty()) return original;
        if (state.getValue(LiquidBlock.LEVEL) != 0) return original;
        if (!(context instanceof EntityCollisionContext entityContext)) return original;
        if (!(entityContext.getEntity() instanceof LivingEntity living)) return original;
        if (!context.isAbove(LiquidBlock.STABLE_SHAPE, pos, true)) return original;
        FluidState own = state.getFluidState();
        if (!living.canStandOnFluid(own)) return original;
        if (getter.getFluidState(pos.above()).getType().isSame(own.getType())) return original;
        return LiquidBlock.STABLE_SHAPE;
    }
}
