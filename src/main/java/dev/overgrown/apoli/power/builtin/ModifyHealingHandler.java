package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public final class ModifyHealingHandler {
    private ModifyHealingHandler() {}

    public static float modify(Entity entity, float original) {
        List<AttributeModifier> mods = new ArrayList<>();
        PowerLookup.forEach(entity, Apoli.id("modify_healing"), ModifyHealingPower.Config.class, cfg -> {
            cfg.modifier().ifPresent(mods::add);
            cfg.modifiers().ifPresent(mods::addAll);
        });
        if (mods.isEmpty()) {
            return original;
        }
        return AttributeModifierHelper.apply(original, mods, entity);
    }
}
