package dev.overgrown.apoli.compat.flywheel;

import dev.engine_room.flywheel.api.visual.EntityVisual;
import dev.engine_room.flywheel.api.visualization.EntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.world.entity.Entity;

public final class ApoliEntityVisualizer<T extends Entity> implements EntityVisualizer<T> {
    private final EntityVisualizer<? super T> delegate;

    public ApoliEntityVisualizer(EntityVisualizer<? super T> delegate) {
        this.delegate = delegate;
    }

    public EntityVisualizer<? super T> delegate() {
        return this.delegate;
    }

    @Override
    public EntityVisual<? super T> createVisual(VisualizationContext ctx, T entity, float partialTick) {
        if (((VanillaRenderClaim) entity).apoli$vanillaClaimed()) return null;
        return this.delegate.createVisual(ctx, entity, partialTick);
    }

    @Override
    public boolean skipVanillaRender(T entity) {
        return !((VanillaRenderClaim) entity).apoli$vanillaClaimed() && this.delegate.skipVanillaRender(entity);
    }
}
