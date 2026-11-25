package dev.overgrown.apoli.compat.gun.condition;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.compat.gun.GunBridge;
import dev.overgrown.apoli.compat.gun.GunFilter;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.LivingEntity;

public final class AimingCondition implements ConditionType<EntityCtx, GunFilter> {

    @Override
    public MapCodec<GunFilter> codec() {
        return GunFilter.MAP_CODEC;
    }

    @Override
    public boolean test(GunFilter filter, EntityCtx ctx) {
        if (!(ctx.raw() instanceof LivingEntity living) || !GunBridge.isAiming(living)) return false;
        return filter.test(GunBridge.gunId(living.getMainHandItem()));
    }
}
