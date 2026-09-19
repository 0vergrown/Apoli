package dev.overgrown.apoli.effects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;

public final class CustomEffectRegistry {
    public static final HashMap<ResourceLocation, CustomEffect> byId = new HashMap<>();
    public static final HashMap<CustomEffect, ResourceLocation> byEffect = new HashMap<>();

    public static void register(CustomEffect effect) {
        byId.put(effect.id(), effect);
        byEffect.put(effect, effect.id());

        ((RuntimeMobEffectRegistry) BuiltInRegistries.MOB_EFFECT).apoli$register(effect);
    }

    public static void replaceAll(List<CustomEffect> effects) {
        clear();
        effects.forEach(CustomEffectRegistry::register);
    }

    public static void clear() {
        ((RuntimeMobEffectRegistry) BuiltInRegistries.MOB_EFFECT).apoli$truncate(byEffect.keySet().stream().sorted().toList());

        byId.clear();
        byEffect.clear();
    }

    public static int size() {
        return byId.size();
    }
}
