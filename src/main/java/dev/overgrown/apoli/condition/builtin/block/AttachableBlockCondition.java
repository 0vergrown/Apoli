package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class AttachableBlockCondition implements ConditionType<BlockCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, BlockCtx ctx) {
        BlockPos here = ctx.pos();
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = here.relative(dir);
            BlockState neighborState = ctx.level().getBlockState(neighbor);
            if (neighborState.isFaceSturdy(ctx.level(), neighbor, dir.getOpposite())) return true;
        }
        return false;
    }
}
