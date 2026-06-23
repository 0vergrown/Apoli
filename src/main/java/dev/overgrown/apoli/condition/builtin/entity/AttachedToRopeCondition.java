package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.rope.RopeManager;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class AttachedToRopeCondition implements ConditionType<EntityCtx, EmptyCfg> {

    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, EntityCtx ctx) {
        return ctx.raw() != null && RopeManager.get(ctx.raw().getUUID()) != null;
    }
}
