package dev.overgrown.apoli.compat.flywheel;

import dev.engine_room.flywheel.api.visualization.EntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualManager;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class FlywheelBridge {
    private static final int WRAP_INTERVAL = 20;

    private static int ticksUntilWrap;
    private static ClientLevel lastLevel;

    private FlywheelBridge() {}

    public static void clientTick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            lastLevel = null;
            return;
        }
        if (level != lastLevel) {
            lastLevel = level;
            ticksUntilWrap = 0;
        }
        if (--ticksUntilWrap <= 0) {
            ticksUntilWrap = WRAP_INTERVAL;
            wrapVisualizers();
        }
        if (!VisualizationManager.supportsVisualization(level)) return;
        VisualizationManager manager = VisualizationManager.get(level);
        if (manager == null) return;
        VisualManager<Entity> entities = manager.entities();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(VisualizerRegistry.getVisualizer(entity.getType()) instanceof ApoliEntityVisualizer<?>)) continue;
            VanillaRenderClaim claim = (VanillaRenderClaim) entity;
            boolean vanilla = ApoliRenderClaims.needsVanillaRenderer(mc, entity);
            if (vanilla == claim.apoli$vanillaClaimed()) continue;
            claim.apoli$setVanillaClaimed(vanilla);
            if (vanilla) {
                entities.queueRemove(entity);
            } else {
                entities.queueAdd(entity);
            }
        }
        CustomProjectileVisualizer.clientTick(mc);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void wrapVisualizers() {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            EntityVisualizer<?> visualizer = VisualizerRegistry.getVisualizer(type);
            if (visualizer == null || visualizer instanceof ApoliEntityVisualizer<?>) continue;
            VisualizerRegistry.setVisualizer((EntityType) type, new ApoliEntityVisualizer(visualizer));
        }
    }
}
