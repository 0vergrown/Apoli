package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Shape;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class AreaOfEffectBlockMetaAction implements ActionType<BlockCtx, AreaOfEffectBlockMetaAction.Cfg> {
    public record Cfg(
        Vector radius,
        Shape shape,
        BlockAction blockAction,
        Optional<BlockCondition> blockCondition
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Vector.SCALAR_OR_VECTOR.optionalFieldOf("radius", Vector.uniform(16.0f)).forGetter(Cfg::radius),
            Shape.CODEC.optionalFieldOf("shape", Shape.CUBE).forGetter(Cfg::shape),
            BlockAction.CODEC.fieldOf("block_action").forGetter(Cfg::blockAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Cfg::blockCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        for (BlockPos pos : cfg.shape.positions(ctx.pos(),
                (int) Math.ceil(cfg.radius.x()), (int) Math.ceil(cfg.radius.y()), (int) Math.ceil(cfg.radius.z()))) {
            BlockState state = ctx.level().getBlockState(pos);
            BlockCtx nestedCtx = ctx.at(pos.immutable(), state);
            if (cfg.blockCondition.isPresent() && !cfg.blockCondition.get().test(nestedCtx)) continue;
            cfg.blockAction.run(nestedCtx);
        }
    }
}
