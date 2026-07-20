package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class NothingBiEntityAction implements ActionType<BiEntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, BiEntityCtx ctx) {
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
