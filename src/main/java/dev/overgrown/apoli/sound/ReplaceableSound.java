package dev.overgrown.apoli.sound;

import net.minecraft.resources.ResourceLocation;

public interface ReplaceableSound {
    void apoli$replaceWith(ResourceLocation location, float volume, float pitch);

    float apoli$baseVolume();

    float apoli$basePitch();
}
