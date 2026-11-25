package dev.overgrown.apoli.compat.gun.condition;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.compat.gun.GunBridge;
import dev.overgrown.apoli.compat.gun.GunFilter;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.resources.ResourceLocation;

public final class GunItemCondition implements ConditionType<ItemCtx, GunFilter> {

    @Override
    public MapCodec<GunFilter> codec() {
        return GunFilter.MAP_CODEC;
    }

    @Override
    public boolean test(GunFilter filter, ItemCtx ctx) {
        ResourceLocation gun = GunBridge.gunId(ctx.stack());
        return gun != null && filter.test(gun);
    }
}
