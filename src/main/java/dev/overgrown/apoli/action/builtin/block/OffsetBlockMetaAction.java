package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.condition.context.BlockCtx;
import net.minecraft.core.BlockPos;

public final class OffsetBlockMetaAction implements ActionType<BlockCtx, OffsetBlockMetaAction.Cfg> {
    public record Cfg(BlockAction action, int x, int y, int z) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockAction.CODEC.fieldOf("action").forGetter(Cfg::action),
            Codec.INT.optionalFieldOf("x", 0).forGetter(Cfg::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(Cfg::y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(Cfg::z)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        BlockPos offset = ctx.pos().offset(cfg.x, cfg.y, cfg.z);
        cfg.action.run(new BlockCtx(offset, ctx.level().getBlockState(offset), ctx.level()));
    }
}
