package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public final class ModifySwimSpeedHandler {
    private ModifySwimSpeedHandler() {}

    private static final float MAX_DRAG = 0.96f;

    public static float modifyWaterDrag(LivingEntity entity, float original) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return original;
        boolean own = !container.powersOfType(ApoliIds.MODIFY_SWIM_SPEED).isEmpty();
        boolean transferred = AttributeModifyTransferPower.SWIM_SPEED.appliesTo(container);
        if (!own && !transferred) return original;

        List<AttributeModifier> mods = List.of();
        if (own) {
            List<AttributeModifier> collected = new ArrayList<>();
            PowerLookup.forEach(entity, ApoliIds.MODIFY_SWIM_SPEED, ModifySwimSpeedPower.Config.class, cfg ->
                collected.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers())));
            mods = collected;
        }
        if (transferred) mods = AttributeModifyTransferPower.SWIM_SPEED.merge(entity, container, mods);
        if (mods.isEmpty()) return original;

        float modified = AttributeModifierHelper.apply(original, AttributeModifierHelper.ensureSorted(mods), entity);
        return Mth.clamp(modified, 0.0f, MAX_DRAG);
    }
}
