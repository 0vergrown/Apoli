package dev.overgrown.apoli.condition.context;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public record BiomeCtx(Holder<Biome> biome, BlockPos pos, Level level) {}
