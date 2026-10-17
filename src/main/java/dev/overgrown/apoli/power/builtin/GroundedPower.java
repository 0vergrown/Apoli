package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeGate;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.LivingEntity;

public final class GroundedPower extends PowerType<EmptyCfg> {
    private static final PowerTypeGate IN_USE = new PowerTypeGate(ApoliIds.GROUNDED);

    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    public static boolean holds(LivingEntity entity) {
        return IN_USE.inUse() && PowerLookup.hasActive(entity, ApoliIds.GROUNDED);
    }
}
