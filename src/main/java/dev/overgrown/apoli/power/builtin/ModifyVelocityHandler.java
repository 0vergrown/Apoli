package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.builtin.bientity.RelativeRotationCondition;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ModifyVelocityHandler {
    private ModifyVelocityHandler() {}

    
    public static Vec3 modify(Entity entity, Vec3 original) {
        List<AttributeModifier> xMods = new ArrayList<>();
        List<AttributeModifier> yMods = new ArrayList<>();
        List<AttributeModifier> zMods = new ArrayList<>();
        PowerLookup.forEach(entity, Apoli.id("modify_velocity"), ModifyVelocityPower.Config.class, cfg -> {
            List<AttributeModifier> mods = new ArrayList<>();
            cfg.modifier().ifPresent(mods::add);
            cfg.modifiers().ifPresent(mods::addAll);
            if (mods.isEmpty()) return;
            if (cfg.axes().contains(RelativeRotationCondition.Axis.X)) xMods.addAll(mods);
            if (cfg.axes().contains(RelativeRotationCondition.Axis.Y)) yMods.addAll(mods);
            if (cfg.axes().contains(RelativeRotationCondition.Axis.Z)) zMods.addAll(mods);
        });
        if (xMods.isEmpty() && yMods.isEmpty() && zMods.isEmpty()) {
            return original;
        }
        LivingEntity living = entity instanceof LivingEntity le ? le : null;
        return new Vec3(
            AttributeModifierHelper.apply(original.x, xMods, living),
            AttributeModifierHelper.apply(original.y, yMods, living),
            AttributeModifierHelper.apply(original.z, zMods, living)
        );
    }
}
