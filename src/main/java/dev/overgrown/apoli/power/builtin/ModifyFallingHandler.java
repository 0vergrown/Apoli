package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class ModifyFallingHandler {
    private ModifyFallingHandler() {}

    public static double modifyGravity(LivingEntity entity, double original) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return original;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.MODIFY_FALLING);
        if (powers.isEmpty()) return original;
        if (entity.getDeltaMovement().y > 0.0) return original;

        double gravity = original;
        boolean applied = false;
        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof ModifyFallingPower.Config cfg)) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            applied = true;
            gravity = Math.min(gravity, cfg.velocity());
            if (!cfg.takeFallDamage()) {
                entity.fallDistance = 0.0f;
            }
        }
        return applied ? gravity : original;
    }
}
