package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.Entity;

public final class RidingRecursiveCondition implements ConditionType<BiEntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, BiEntityCtx ctx) {
        if (!ctx.actor().isPassenger()) return false;
        Entity vehicle = ctx.actor().getVehicle();
        while (vehicle != null && !vehicle.equals(ctx.target())) {
            vehicle = vehicle.getVehicle();
        }
        return ctx.target().equals(vehicle);
    }
}
