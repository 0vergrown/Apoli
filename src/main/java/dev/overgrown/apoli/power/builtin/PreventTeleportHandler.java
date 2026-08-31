package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.Entity;

public final class PreventTeleportHandler {
    private PreventTeleportHandler() {}

    private static boolean firing;
    private static int lastEntityId = -1;
    private static long lastTick = Long.MIN_VALUE;

    public static boolean prevented(Entity entity, boolean dimensionChange) {
        if (entity == null || entity.level().isClientSide()) return false;
        if (!PowerLookup.hasActive(entity, ApoliIds.PREVENT_TELEPORT)) return false;

        long tick = entity.level().getGameTime();
        boolean notify = !firing && (lastEntityId != entity.getId() || lastTick != tick);
        boolean[] blocked = {false};
        if (notify) firing = true;
        try {
            PowerLookup.forEach(entity, ApoliIds.PREVENT_TELEPORT, PreventTeleportPower.Config.class, config -> {
                if (dimensionChange && !config.preventDimensionChange()) return;
                blocked[0] = true;
                if (notify) config.entityAction().ifPresent(action -> action.run(new EntityCtx(entity, entity.level())));
            });
        } finally {
            if (notify) firing = false;
        }
        if (blocked[0] && notify) {
            lastEntityId = entity.getId();
            lastTick = tick;
        }
        return blocked[0];
    }
}
