package dev.overgrown.apoli.data;

import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public final class ExpressionContext {
    private ExpressionContext() {}

    public static Map<String, Double> entityStats(LivingEntity entity) {
        Map<String, Double> m = new HashMap<>();
        m.put("health", (double) entity.getHealth());
        m.put("max_health", (double) entity.getMaxHealth());
        m.put("air", (double) entity.getAirSupply());
        if (entity instanceof Player p) {
            m.put("food", (double) p.getFoodData().getFoodLevel());
            m.put("xp_level", (double) p.experienceLevel);
            m.put("xp_progress", (double) p.experienceProgress);
        }
        if (entity.level() instanceof ServerLevel sl) {
            m.put("world_time", (double) sl.getGameTime());
            m.put("day_time", (double) sl.getDayTime());
        }
        return m;
    }

    public static Map<String, Double> resourceValues(PowerContainer container) {
        Map<String, Double> m = new HashMap<>();
        for (ResourceLocation powerId : container.allPowers()) {
            container.getAuxInt(powerId).ifPresent(v -> m.put(powerId.toString(), (double) v));
        }
        return m;
    }

    public static Map<String, Double> forResource(LivingEntity entity, int currentValue) {
        Map<String, Double> m = entityStats(entity);
        m.put("value", (double) currentValue);
        return m;
    }

    public static OptionalInt readResourceValue(PowerContainer container, ResourceLocation powerId) {
        if (!container.hasPower(powerId)) return OptionalInt.empty();
        Power p = ApoliPowers.get(powerId);
        if (p == null) return OptionalInt.empty();
        return container.getAuxInt(powerId);
    }
}
