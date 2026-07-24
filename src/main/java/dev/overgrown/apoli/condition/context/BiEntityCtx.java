package dev.overgrown.apoli.condition.context;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class BiEntityCtx {
    private final Entity actor;
    private final Entity target;
    private final Level level;

    public BiEntityCtx(Entity actor, Entity target, Level level) {
        this.actor = actor;
        this.target = target;
        this.level = level;
    }

    public static BiEntityCtx of(Entity actor, Entity target, Level level) {
        return new BiEntityCtx(actor, target, level);
    }

    public Entity actor() {
        return actor;
    }

    public Entity target() {
        return target;
    }

    public LivingEntity livingActor() {
        return actor instanceof LivingEntity le ? le : null;
    }

    public LivingEntity livingTarget() {
        return target instanceof LivingEntity le ? le : null;
    }

    public Level level() {
        return level;
    }

    public Entity rawActor() {
        return actor;
    }

    public Entity rawTarget() {
        return target;
    }

    public EntityCtx asActor() {
        return new EntityCtx(actor, level);
    }

    public EntityCtx asTarget() {
        return new EntityCtx(target, level);
    }

    public BiEntityCtx swap() {
        return new BiEntityCtx(target, actor, level);
    }
}
