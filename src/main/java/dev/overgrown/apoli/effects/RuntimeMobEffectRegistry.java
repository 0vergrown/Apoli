package dev.overgrown.apoli.effects;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public interface RuntimeMobEffectRegistry {
    void apoli$unregisterCustom(Set<ResourceLocation> keep);
    CustomMobEffect apoli$register(CustomMobEffect effect);
}
