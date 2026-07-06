package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Shape;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.core.BlockPos;

public final class BlockInRadiusCondition implements ConditionType<EntityCtx, BlockInRadiusCondition.Cfg> {
    public record Cfg(BlockCondition blockCondition, Vector radius, Shape shape, Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.fieldOf("block_condition").forGetter(Cfg::blockCondition),
            Vector.SCALAR_OR_VECTOR.fieldOf("radius").forGetter(Cfg::radius),
            Shape.CODEC.optionalFieldOf("shape", Shape.CUBE).forGetter(Cfg::shape),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison),
            Codec.INT.optionalFieldOf("compare_to", 1).forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        BlockPos center = ctx.entity().blockPosition();
        int count = 0;
        for (BlockPos pos : cfg.shape.positions(center,
                (int) Math.ceil(cfg.radius.x()), (int) Math.ceil(cfg.radius.y()), (int) Math.ceil(cfg.radius.z()))) {
            if (cfg.blockCondition.test(new BlockCtx(pos, ctx.level().getBlockState(pos), ctx.level()))) count++;
        }
        return cfg.comparison.compare(count, cfg.compareTo);
    }
}
