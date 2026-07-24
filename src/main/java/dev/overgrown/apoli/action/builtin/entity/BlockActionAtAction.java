package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.BlockPos;

public final class BlockActionAtAction implements ActionType<EntityCtx, BlockActionAtAction.Cfg> {
    public record Cfg(BlockAction blockAction) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockAction.CODEC.fieldOf("block_action").forGetter(Cfg::blockAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        BlockPos pos = ctx.raw().blockPosition();
        cfg.blockAction.run(new BlockCtx(pos, ctx.level().getBlockState(pos), ctx.level()));
    }
}
