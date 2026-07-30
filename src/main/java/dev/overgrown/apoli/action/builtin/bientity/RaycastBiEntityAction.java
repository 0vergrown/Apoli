package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.builtin.entity.RaycastAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;

public final class RaycastBiEntityAction implements ActionType<BiEntityCtx, RaycastAction.Cfg> {

    @Override
    public MapCodec<RaycastAction.Cfg> codec() {
        return RaycastAction.CONFIG_CODEC;
    }

    @Override
    public void run(RaycastAction.Cfg cfg, BiEntityCtx ctx) {
        RaycastAction.runTowards(cfg, ctx.asActor(), ctx.target());
    }
}
