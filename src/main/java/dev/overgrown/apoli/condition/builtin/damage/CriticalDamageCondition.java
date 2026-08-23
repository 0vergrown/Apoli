package dev.overgrown.apoli.condition.builtin.damage;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.data.CriticalHitContext;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class CriticalDamageCondition implements ConditionType<DamageCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, DamageCtx ctx) {
        return CriticalHitContext.isCritical(ctx.source().getEntity());
    }
}
