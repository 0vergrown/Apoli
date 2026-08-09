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
        if (container.powersOfType(ApoliIds.MODIFY_SWIM_SPEED).isEmpty()) return original;

        List<AttributeModifier> mods = new ArrayList<>();
        PowerLookup.forEach(entity, ApoliIds.MODIFY_SWIM_SPEED, ModifySwimSpeedPower.Config.class, cfg ->
            mods.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers())));
        if (mods.isEmpty()) return original;

        float modified = AttributeModifierHelper.apply(original, AttributeModifierHelper.ensureSorted(mods), entity);
        return Mth.clamp(modified, 0.0f, MAX_DRAG);
    }
}
