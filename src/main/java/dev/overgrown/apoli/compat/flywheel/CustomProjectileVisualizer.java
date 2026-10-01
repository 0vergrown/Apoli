package dev.overgrown.apoli.compat.flywheel;

import dev.engine_room.flywheel.api.visual.EntityVisual;
import dev.engine_room.flywheel.api.visualization.EntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.entity.CustomProjectileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomProjectileVisualizer implements EntityVisualizer<CustomProjectileEntity> {
    private static final CustomProjectileVisualizer INSTANCE = new CustomProjectileVisualizer();
    private static final Map<CustomProjectileEntity, CustomProjectileVisual> LIVE = new ConcurrentHashMap<>();
    private static volatile boolean disabled;

    private CustomProjectileVisualizer() {}

    public static void register(EntityType<CustomProjectileEntity> type) {
        VisualizerRegistry.setVisualizer(type, INSTANCE);
    }

    public static void clientTick(Minecraft mc) {
        if (disabled || LIVE.isEmpty()) return;
        try {
            for (CustomProjectileVisual visual : LIVE.values()) {
                visual.prepare(mc);
            }
        } catch (RuntimeException | LinkageError e) {
            disable(e);
        }
    }

    static boolean disabled() {
        return disabled;
    }

    static void forget(CustomProjectileEntity entity, CustomProjectileVisual visual) {
        LIVE.remove(entity, visual);
    }

    static void disable(Throwable cause) {
        if (disabled) return;
        disabled = true;
        Apoli.LOGGER.error("[Apoli] Drawing custom projectiles through Flywheel failed, so they draw the vanilla way "
            + "for the rest of the session.", cause);
    }

    @Override
    public EntityVisual<? super CustomProjectileEntity> createVisual(VisualizationContext ctx, CustomProjectileEntity entity,
                                                                    float partialTick) {
        if (disabled) return null;
        try {
            CustomProjectileVisual visual = new CustomProjectileVisual(ctx, entity, partialTick);
            LIVE.put(entity, visual);
            return visual;
        } catch (RuntimeException | LinkageError e) {
            disable(e);
            return null;
        }
    }

    @Override
    public boolean skipVanillaRender(CustomProjectileEntity entity) {
        if (disabled) return false;
        CustomProjectileVisual visual = LIVE.get(entity);
        return visual != null && visual.drawn();
    }
}
