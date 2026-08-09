package dev.overgrown.apoli.entity.summon;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface Temporary {
    void setMaxLifeTime(int ticks);

    int getMaxLifeTime();

    int getRemainingLifeTime();

    @Nullable
    ResourceLocation getSummonId();
}
