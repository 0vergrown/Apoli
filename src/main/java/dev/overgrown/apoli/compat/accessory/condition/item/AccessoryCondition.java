package dev.overgrown.apoli.compat.accessory.condition.item;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;


public final class AccessoryCondition implements ConditionType<ItemCtx, AccessoryCondition.Cfg> {
    public record Cfg() {}

    @Override
    public MapCodec<Cfg> codec() {
        return MapCodec.unit(new Cfg());
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        return Accessories.isAccessory(ctx.stack(), ctx.level());
    }
}
