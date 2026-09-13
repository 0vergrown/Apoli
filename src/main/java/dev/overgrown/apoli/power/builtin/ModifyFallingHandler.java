package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
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
        List<AttributeModifier> mods = null;
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
            if (cfg.velocity().isPresent()) {
                gravity = Math.min(gravity, cfg.velocity().get().evalWith(entity, container, original));
            }
            if (cfg.modifier().isPresent()) {
                if (mods == null) mods = new ArrayList<>(2);
                mods.add(cfg.modifier().get());
            }
            if (cfg.modifiers().isPresent()) {
                if (mods == null) mods = new ArrayList<>(4);
                mods.addAll(cfg.modifiers().get());
            }
        }
        if (!applied) return original;
        if (mods != null) gravity = AttributeModifierHelper.apply(gravity, mods, entity, container);
        return gravity;
    }

    public static void applyLandingImmunity(LivingEntity entity) {
        if (entity.fallDistance <= 0.0f) return;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.MODIFY_FALLING);
        if (powers.isEmpty()) return;

        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof ModifyFallingPower.Config cfg)) continue;
            if (cfg.takeFallDamage()) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            entity.fallDistance = 0.0f;
            return;
        }
    }
}
