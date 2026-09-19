package dev.overgrown.apoli.effects;

import java.util.List;

public interface RuntimeMobEffectRegistry {
    void apoli$truncate(List<CustomEffect> effects);

    void apoli$register(CustomEffect effect);
}
