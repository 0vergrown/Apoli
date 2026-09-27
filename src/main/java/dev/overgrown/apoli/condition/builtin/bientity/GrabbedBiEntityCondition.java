package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.entity.GrabManager;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class GrabbedBiEntityCondition implements ConditionType<BiEntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, BiEntityCtx ctx) {
        if (ctx.actor() == null || ctx.target() == null || ctx.level().isClientSide()) return false;
        return GrabManager.isGrabbedBy(ctx.target().getUUID(), ctx.actor().getUUID());
    }
}
