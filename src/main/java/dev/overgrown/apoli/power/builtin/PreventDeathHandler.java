package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class PreventDeathHandler {
    private PreventDeathHandler() {}

    public static boolean tryPrevent(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;

        DamageCtx damageCtx = null;
        boolean prevented = false;

        for (ResourceLocation powerId : container.allPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (container.isSuppressed(powerId)) continue;
            if (!(PowerTypeRegistry.get(power.typeId()) instanceof PreventDeathPower)) continue;
            if (!(power.config() instanceof PreventDeathPower.Config cfg)) continue;
            if (power.condition().isPresent()
                && !power.condition().get().test(new EntityCtx(entity, level))) continue;
            if (cfg.damageCondition().isPresent()) {
                if (damageCtx == null) damageCtx = new DamageCtx(source, entity, level, amount);
                if (!cfg.damageCondition().get().test(damageCtx)) continue;
            }

            if (!prevented) {
                entity.setHealth(1.0F);
                prevented = true;
            }
            if (cfg.entityAction().isPresent()) {
                cfg.entityAction().get().run(new EntityCtx(entity, level));
            }
        }
        return prevented;
    }
}
