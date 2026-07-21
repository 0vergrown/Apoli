package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class EqualCondition implements ConditionType<BiEntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, BiEntityCtx ctx) {
        return ctx.rawActor() != null && ctx.rawActor() == ctx.rawTarget();
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
