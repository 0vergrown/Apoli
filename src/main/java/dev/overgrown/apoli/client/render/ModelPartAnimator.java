package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.data.ModelPartTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ModelPartAnimator {
    private ModelPartAnimator() {}

    private static final ModelPartTimeline TIMELINE = new ModelPartTimeline();

    public static List<ModelPartTimeline.Slot> update(@Nullable LivingEntity entity) {
        if (entity == null) return List.of();
        return TIMELINE.update(entity, now(entity));
    }

    public static boolean overridesPose(@Nullable LivingEntity entity) {
        if (entity == null) return false;
        return TIMELINE.overridesPose(entity, now(entity));
    }

    public static float ageInTicks(@Nullable Entity entity) {
        return entity == null ? 0.0F : (float) now(entity);
    }

    private static double now(Entity entity) {
        return entity.tickCount + (double) Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }
}
