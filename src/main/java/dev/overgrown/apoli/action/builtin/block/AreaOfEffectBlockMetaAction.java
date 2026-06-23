package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Shape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class AreaOfEffectBlockMetaAction implements ActionType<BlockCtx, AreaOfEffectBlockMetaAction.Cfg> {
    public record Cfg(
        int radius,
        Shape shape,
        BlockAction blockAction,
        Optional<BlockCondition> blockCondition
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("radius", 16).forGetter(Cfg::radius),
            Shape.CODEC.optionalFieldOf("shape", Shape.CUBE).forGetter(Cfg::shape),
            BlockAction.CODEC.fieldOf("block_action").forGetter(Cfg::blockAction),
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Cfg::blockCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        for (BlockPos pos : cfg.shape.positions(ctx.pos(), cfg.radius)) {
            BlockState state = ctx.level().getBlockState(pos);
            BlockCtx nestedCtx = new BlockCtx(pos.immutable(), state, ctx.level());
            if (cfg.blockCondition.isPresent() && !cfg.blockCondition.get().test(nestedCtx)) continue;
            cfg.blockAction.run(nestedCtx);
        }
    }
}
