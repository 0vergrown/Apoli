package dev.overgrown.apoli.condition.context;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class BiEntityCtx {
    private final Entity actor;
    private final Entity target;
    private final Level level;

    public BiEntityCtx(LivingEntity actor, LivingEntity target, Level level) {
        this.actor = actor;
        this.target = target;
        this.level = level;
    }

    private BiEntityCtx(Level level, Entity actor, Entity target) {
        this.actor = actor;
        this.target = target;
        this.level = level;
    }

    public static BiEntityCtx of(Entity actor, Entity target, Level level) {
        return new BiEntityCtx(level, actor, target);
    }

    public LivingEntity actor() {
        return actor instanceof LivingEntity le ? le : null;
    }

    public LivingEntity target() {
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
        return EntityCtx.of(actor, level);
    }

    public EntityCtx asTarget() {
        return EntityCtx.of(target, level);
    }

    public BiEntityCtx swap() {
        return new BiEntityCtx(level, target, actor);
    }
}
