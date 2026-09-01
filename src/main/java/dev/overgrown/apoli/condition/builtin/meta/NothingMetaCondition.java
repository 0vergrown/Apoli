package dev.overgrown.apoli.condition.builtin.meta;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class NothingMetaCondition<CTX> implements ConditionType<CTX, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, CTX ctx) {
        return true;
    }
}
