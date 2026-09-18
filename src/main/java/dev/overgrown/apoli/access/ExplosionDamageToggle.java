package dev.overgrown.apoli.access;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface ExplosionDamageToggle {
    void apoli$sparePassersby(@Nullable Entity exempt);

    void apoli$setDamageFilter(@Nullable Predicate<Entity> filter);
}
