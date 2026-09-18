package dev.overgrown.apoli.condition.context;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record BlockCtx(BlockPos pos, BlockState state, Level level, @Nullable Entity actor, @Nullable Vec3 hit) {

    public BlockCtx(BlockPos pos, BlockState state, Level level) {
        this(pos, state, level, null, null);
    }

    public BlockCtx(BlockPos pos, BlockState state, Level level, @Nullable Entity actor) {
        this(pos, state, level, actor, null);
    }

    public BlockCtx at(BlockPos pos, BlockState state) {
        return new BlockCtx(pos, state, this.level, this.actor, null);
    }

    public BlockCtx withHit(@Nullable Vec3 hit) {
        return new BlockCtx(this.pos, this.state, this.level, this.actor, hit);
    }
}
