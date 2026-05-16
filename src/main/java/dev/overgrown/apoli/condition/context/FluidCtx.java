package dev.overgrown.apoli.condition.context;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

public record FluidCtx(FluidState state, BlockPos pos, Level level) {}
