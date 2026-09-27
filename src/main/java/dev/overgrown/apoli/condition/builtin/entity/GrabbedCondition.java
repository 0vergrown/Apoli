package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.GrabManager;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class GrabbedCondition implements ConditionType<EntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, EntityCtx ctx) {
        if (ctx.entity() == null || ctx.level().isClientSide()) return false;
        return GrabManager.isGrabbed(ctx.entity().getUUID());
    }
}
