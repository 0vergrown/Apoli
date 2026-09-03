package dev.overgrown.apoli.condition.context;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record BlockCtx(BlockPos pos, BlockState state, Level level, @Nullable Entity actor) {

    public BlockCtx(BlockPos pos, BlockState state, Level level) {
        this(pos, state, level, null);
    }

    public BlockCtx at(BlockPos pos, BlockState state) {
        return new BlockCtx(pos, state, this.level, this.actor);
    }
}
