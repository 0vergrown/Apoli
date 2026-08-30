package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class AdjacentBlockCondition implements ConditionType<BlockCtx, AdjacentBlockCondition.Cfg> {
    public record Cfg(BlockCondition adjacentCondition, Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.fieldOf("adjacent_condition").forGetter(Cfg::adjacentCondition),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        int matches = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = ctx.pos().relative(dir);
            BlockCtx neighborCtx = new BlockCtx(neighbor, ctx.level().getBlockState(neighbor), ctx.level());
            if (cfg.adjacentCondition.test(neighborCtx)) matches++;
        }
        return cfg.comparison.compare(matches, cfg.compareTo);
    }
}
