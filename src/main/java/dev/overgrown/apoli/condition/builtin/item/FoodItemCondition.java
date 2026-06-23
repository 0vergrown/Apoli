package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class FoodItemCondition implements ConditionType<ItemCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, ItemCtx ctx) {
        return ctx.stack().getItem().isEdible();
    }
}
