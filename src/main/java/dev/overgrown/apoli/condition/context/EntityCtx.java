package dev.overgrown.apoli.condition.context;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public record EntityCtx(Entity entity, Level level) {

    public static EntityCtx of(Entity entity, Level level) {
        return new EntityCtx(entity, level);
    }

    public LivingEntity living() {
        return entity instanceof LivingEntity le ? le : null;
    }

    public Entity raw() {
        return entity;
    }
}
