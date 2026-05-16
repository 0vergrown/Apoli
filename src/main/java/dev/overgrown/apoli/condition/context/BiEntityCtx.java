package dev.overgrown.apoli.condition.context;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public record BiEntityCtx(LivingEntity actor, LivingEntity target, Level level) {
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
