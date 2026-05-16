package dev.overgrown.apoli.condition.context;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public record DamageCtx(DamageSource source, LivingEntity target, Level level, float amount) {}
