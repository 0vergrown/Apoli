package dev.overgrown.apoli.access;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface KeyedLootTable {
    @Nullable
    ResourceLocation apoli$key();

    void apoli$setKey(ResourceLocation key);
}
