package dev.overgrown.apoli.condition.context;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public record EntityCtx(LivingEntity entity, Level level) {}
