package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.disguise.DisguiseManager;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class RemoveDisguiseAction implements ActionType<EntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, EntityCtx ctx) {
        if (ctx.entity() != null) DisguiseManager.remove(ctx.entity());
    }
}
