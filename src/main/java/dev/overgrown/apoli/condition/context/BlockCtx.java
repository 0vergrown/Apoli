package dev.overgrown.apoli.condition.context;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record BlockCtx(BlockPos pos, BlockState state, Level level) {}
