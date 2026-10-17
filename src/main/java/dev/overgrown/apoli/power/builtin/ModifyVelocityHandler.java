package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.builtin.bientity.RelativeRotationCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ModifyVelocityHandler {
    private ModifyVelocityHandler() {}

    public static Vec3 modify(Entity entity, Vec3 movement) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return movement;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.MODIFY_VELOCITY);
        if (powers.isEmpty()) return movement;

        ModifyVelocityPower.Config first = null;
        List<ModifyVelocityPower.Config> several = null;
        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof ModifyVelocityPower.Config cfg)) continue;
            if (cfg.combined().isEmpty()) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            if (first == null) {
                first = cfg;
                continue;
            }
            if (several == null) {
                several = new ArrayList<>(4);
                several.add(first);
            }
            several.add(cfg);
        }
        if (first == null) return movement;

        List<AttributeModifier> xMods = modifiersFor(RelativeRotationCondition.Axis.X, first, several);
        List<AttributeModifier> yMods = modifiersFor(RelativeRotationCondition.Axis.Y, first, several);
        List<AttributeModifier> zMods = modifiersFor(RelativeRotationCondition.Axis.Z, first, several);
        LivingEntity living = entity instanceof LivingEntity le ? le : null;
        PowerContainer source = living == null ? null : container;

        Vec3 velocity = entity.getDeltaMovement();
        if (movement == velocity || movement.equals(velocity)) {
            double vx = AttributeModifierHelper.applyBounds(movement.x, xMods, living, source);
            double vy = AttributeModifierHelper.applyBounds(movement.y, yMods, living, source);
            double vz = AttributeModifierHelper.applyBounds(movement.z, zMods, living, source);
            if (vx != movement.x || vy != movement.y || vz != movement.z) {
                entity.setDeltaMovement(vx, vy, vz);
            }
        }

        double x = AttributeModifierHelper.apply(movement.x, xMods, living, source);
        double y = AttributeModifierHelper.apply(movement.y, yMods, living, source);
        double z = AttributeModifierHelper.apply(movement.z, zMods, living, source);
        if (x == movement.x && y == movement.y && z == movement.z) return movement;
        return new Vec3(x, y, z);
    }

    private static List<AttributeModifier> modifiersFor(RelativeRotationCondition.Axis axis,
                                                        ModifyVelocityPower.Config first,
                                                        @Nullable List<ModifyVelocityPower.Config> several) {
        if (several == null) return first.affects(axis) ? first.combined() : List.of();
        List<AttributeModifier> merged = null;
        for (int i = 0; i < several.size(); i++) {
            ModifyVelocityPower.Config cfg = several.get(i);
            if (!cfg.affects(axis)) continue;
            if (merged == null) merged = new ArrayList<>();
            merged.addAll(cfg.combined());
        }
        return merged == null ? List.of() : AttributeModifierHelper.ensureSorted(merged);
    }
}
