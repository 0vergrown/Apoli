package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public final class ModifyHealingHandler {
    private ModifyHealingHandler() {}

    public static float modify(LivingEntity entity, float original) {
        if (original <= 0.0f) return original;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return original;
        boolean own = !container.powersOfType(ApoliIds.MODIFY_HEALING).isEmpty();
        boolean transferred = AttributeModifyTransferPower.HEALING.appliesTo(container);
        if (!own && !transferred) return original;

        List<AttributeModifier> mods = List.of();
        if (own) {
            List<AttributeModifier> collected = new ArrayList<>();
            PowerLookup.forEach(entity, ApoliIds.MODIFY_HEALING, ModifyHealingPower.Config.class, cfg -> {
                cfg.modifier().ifPresent(collected::add);
                cfg.modifiers().ifPresent(collected::addAll);
            });
            mods = collected;
        }
        if (transferred) mods = AttributeModifyTransferPower.HEALING.merge(entity, container, mods);
        if (mods.isEmpty()) return original;
        return Math.max(0.0f, AttributeModifierHelper.apply(original, AttributeModifierHelper.ensureSorted(mods), entity));
    }
}
