package dev.overgrown.apoli.condition.context;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public record EntityCtx(LivingEntity entity, Level level, Entity raw) {
    public EntityCtx(LivingEntity entity, Level level) {
        this(entity, level, entity);
    }

    public static EntityCtx of(Entity entity, Level level) {
        return new EntityCtx(entity instanceof LivingEntity le ? le : null, level, entity);
    }
}
