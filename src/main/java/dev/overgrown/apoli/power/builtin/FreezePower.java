package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class FreezePower extends PowerType<EmptyCfg> {
    public static final ResourceLocation CANONICAL = Apoli.id("freeze");

    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void tick(ResourceLocation powerId, EmptyCfg cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (owner == null) return;
        if (!conditionHolds(owner, powerId)) return;
        int threshold = owner.getTicksRequiredToFreeze();
        if (owner.getTicksFrozen() < threshold + 1) {
            owner.setTicksFrozen(threshold + 1);
        }
    }

    private static boolean conditionHolds(LivingEntity entity, ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || loaded.condition().isEmpty()) return true;
        if (!(entity.level() instanceof ServerLevel level)) return true;
        return loaded.condition().get().test(new EntityCtx(entity, level));
    }

    public static boolean isFrozen(LivingEntity entity) {
        return PowerLookup.hasActive(entity, CANONICAL);
    }
}
