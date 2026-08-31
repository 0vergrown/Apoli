package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class NothingMetaAction<CTX> implements ActionType<CTX, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, CTX ctx) {
    }
}
