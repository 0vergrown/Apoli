package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.PhasingPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class PhasingMixin {

    @ModifyReturnValue(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"))
    private VoxelShape apoli$phaseThrough(VoxelShape original, BlockGetter getter, BlockPos pos, CollisionContext ctx) {
        if (original.isEmpty()) return original;
        if (!(ctx instanceof EntityCollisionContext esc)) return original;
        if (!(esc.getEntity() instanceof LivingEntity living)) return original;
        if (!PhasingPower.mayHold(living)) return original;
        Level level = getter instanceof Level fromGetter ? fromGetter : living.level();
        return PhasingPower.phases(living, level, pos, (BlockState) (Object) this, original) ? Shapes.empty() : original;
    }
}
