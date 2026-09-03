package dev.overgrown.apoli.condition.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import net.minecraft.core.BlockPos;

public final class OffsetBlockMetaCondition implements ConditionType<BlockCtx, OffsetBlockMetaCondition.Cfg> {
    public record Cfg(BlockCondition condition, int x, int y, int z) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.fieldOf("condition").forGetter(Cfg::condition),
            Codec.INT.optionalFieldOf("x", 0).forGetter(Cfg::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(Cfg::y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(Cfg::z)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        BlockPos offset = ctx.pos().offset(cfg.x, cfg.y, cfg.z);
        return cfg.condition.test(ctx.at(offset, ctx.level().getBlockState(offset)));
    }
}
