package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.ConnectedBlocks;
import dev.overgrown.apoli.data.expr.ExprContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class ConnectedBlocksBlockMetaAction implements ActionType<BlockCtx, ConnectedBlocksBlockMetaAction.Cfg> {
    private static final int SLOT_COUNT = ExprContext.slot("count");
    private static final int SLOT_INDEX = ExprContext.slot("index");

    public record Cfg(ConnectedBlocks group, BlockAction blockAction, Optional<EntityCondition> actorCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ConnectedBlocks.MAP_CODEC.forGetter(Cfg::group),
            BlockAction.CODEC.fieldOf("block_action").forGetter(Cfg::blockAction),
            LoggedOptionalField.strict("actor_condition", EntityCondition.CODEC).forGetter(Cfg::actorCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        Entity actor = ctx.actor();
        if (cfg.actorCondition.isPresent() && actor == null) return;
        Level level = ctx.level();
        List<BlockPos> blocks = cfg.group.find(level, ctx.pos(), ctx.state(), actor);
        if (blocks.isEmpty()) return;
        EntityCtx actorCtx = cfg.actorCondition.isPresent() ? EntityCtx.of(actor, level) : null;
        double previousCount = ExprContext.push(SLOT_COUNT, blocks.size());
        double previousIndex = ExprContext.get(SLOT_INDEX);
        try {
            for (int i = 0; i < blocks.size(); i++) {
                if (actorCtx != null && !cfg.actorCondition.get().test(actorCtx)) break;
                ExprContext.push(SLOT_INDEX, i);
                BlockPos pos = blocks.get(i);
                cfg.blockAction.run(ctx.at(pos, level.getBlockState(pos)));
            }
        } finally {
            ExprContext.pop(SLOT_INDEX, previousIndex);
            ExprContext.pop(SLOT_COUNT, previousCount);
        }
    }
}
