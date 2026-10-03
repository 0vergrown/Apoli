package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface ProjectileHitActions {
    void apoli$setFireConfig(FireProjectilePower.Config config);

    void apoli$setMaxRange(double blocks);

    void apoli$setLifetime(int ticks);

    void apoli$setFireCause(@Nullable Entity holder, @Nullable ResourceLocation powerId);

    boolean apoli$bouncedThisHit();

    void apoli$caught();
}
