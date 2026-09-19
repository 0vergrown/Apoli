package dev.overgrown.apoli.effects;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public final class CustomEffectRegistry {
    public static final HashMap<ResourceLocation, CustomEffect> byId = new HashMap<>();
    public static final HashMap<CustomEffect, ResourceLocation> byEffect = new HashMap<>();
    public static List<CustomEffect> committed = List.of();

    public static void register(CustomEffect effect) {
        byId.put(effect.id(), effect);
        byEffect.put(effect, effect.id());

        ((RuntimeMobEffectRegistry) BuiltInRegistries.MOB_EFFECT).apoli$register(effect);
    }

    public static void commit(List<CustomEffect> effects) {
        committed = effects;
    }

    public static void clear() {
        ((RuntimeMobEffectRegistry) BuiltInRegistries.MOB_EFFECT).apoli$truncate(byEffect.keySet().stream().sorted(Comparator.comparing(CustomEffect::id)).toList());

        byId.clear();
        byEffect.clear();
    }

    public static int size() {
        return byId.size();
    }

    public static void purge(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity living)) continue;

                List<Holder<MobEffect>> toRemove = living.getActiveEffects().stream()
                        .map(MobEffectInstance::getEffect)
                        .filter(h -> h.unwrapKey().map(k -> byId.containsKey(k.location())).orElse(false))
                        .toList();
                toRemove.forEach(living::removeEffect);
            }
        }
    }

    public static void update(MinecraftServer server) {
        purge(server);
        clear();

        committed.forEach(CustomEffectRegistry::register);

        committed = List.of();
    }
}
