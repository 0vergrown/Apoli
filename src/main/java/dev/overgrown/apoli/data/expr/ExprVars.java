package dev.overgrown.apoli.data.expr;

import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ExprVars {
    private ExprVars() {}

    public record ResolvedVar(ExprVar accessor, boolean needsContainer) {}

    private static final Map<String, ResolvedVar> VARS = new HashMap<>();

    public static void register(String name, ExprVar accessor) {
        VARS.put(name, new ResolvedVar(accessor, false));
    }

    public static @Nullable ResolvedVar resolve(String name) {
        ResolvedVar known = VARS.get(name);
        if (known != null) return known;
        if (name.indexOf(':') >= 0) {
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (id == null) return null;
            return new ResolvedVar((e, c, l, v) -> readResource(c, id), true);
        }
        return null;
    }

    public static double readResource(@Nullable PowerContainer container, ResourceLocation id) {
        return container != null ? container.getAuxIntOr(id, 0) : 0;
    }

    private static Level levelOf(@Nullable LivingEntity entity, @Nullable Level level) {
        if (level != null) return level;
        return entity != null ? entity.level() : null;
    }

    static {
        register("value", (e, c, l, v) -> v);
        register("damage", (e, c, l, v) -> ExprDamageContext.current());

        register("health", (e, c, l, v) -> e != null ? e.getHealth() : 0);
        register("max_health", (e, c, l, v) -> e != null ? e.getMaxHealth() : 0);
        register("absorption", (e, c, l, v) -> e != null ? e.getAbsorptionAmount() : 0);
        register("armor", (e, c, l, v) -> e != null ? e.getArmorValue() : 0);
        register("air", (e, c, l, v) -> e != null ? e.getAirSupply() : 0);
        register("max_air", (e, c, l, v) -> e != null ? e.getMaxAirSupply() : 0);
        register("fall_distance", (e, c, l, v) -> e != null ? e.fallDistance : 0);

        register("x", (e, c, l, v) -> e != null ? e.getX() : 0);
        register("y", (e, c, l, v) -> e != null ? e.getY() : 0);
        register("z", (e, c, l, v) -> e != null ? e.getZ() : 0);
        register("yaw", (e, c, l, v) -> e != null ? e.getYRot() : 0);
        register("pitch", (e, c, l, v) -> e != null ? e.getXRot() : 0);
        register("velocity_x", (e, c, l, v) -> e != null ? e.getDeltaMovement().x : 0);
        register("velocity_y", (e, c, l, v) -> e != null ? e.getDeltaMovement().y : 0);
        register("velocity_z", (e, c, l, v) -> e != null ? e.getDeltaMovement().z : 0);

        register("food", (e, c, l, v) -> e instanceof Player p ? p.getFoodData().getFoodLevel() : 0);
        register("saturation", (e, c, l, v) -> e instanceof Player p ? p.getFoodData().getSaturationLevel() : 0);
        register("xp_level", (e, c, l, v) -> e instanceof Player p ? p.experienceLevel : 0);
        register("xp_progress", (e, c, l, v) -> e instanceof Player p ? p.experienceProgress : 0);

        register("world_time", (e, c, l, v) -> {
            Level level = levelOf(e, l);
            return level != null ? level.getGameTime() : 0;
        });
        register("day_time", (e, c, l, v) -> {
            Level level = levelOf(e, l);
            return level != null ? level.getDayTime() : 0;
        });
        register("moon_phase", (e, c, l, v) -> {
            Level level = levelOf(e, l);
            return level != null ? level.getMoonPhase() : 0;
        });
    }
}
