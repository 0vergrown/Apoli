package dev.overgrown.apoli.client;

import dev.overgrown.apoli.effects.CustomEffect;
import dev.overgrown.apoli.effects.CustomEffectNetworking;
import dev.overgrown.apoli.effects.CustomEffectRegistry;
import dev.overgrown.apoli.effects.RuntimeMobEffectRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SyncCustomEffectRegistry {
    public static void sync(CustomEffectNetworking.SyncCustomEffectsPayload payload) {
        LoggerFactory.getLogger("ClientRegistrySync").info("Received Payload for {} Effects.", payload.effects().size());

        List<CustomEffect> effects = new java.util.ArrayList<>(List.of());

        payload.effects().forEach(effect -> effects.add(CustomEffect.create(effect.id(), List.of(), Vec3.fromRGB24(effect.color()).scale(((double) 1 /255)), effect.icon(), 0, MobEffectCategory.NEUTRAL, effect.name())));

        ((RuntimeMobEffectRegistry) BuiltInRegistries.MOB_EFFECT).apoli$truncate(effects.stream().sorted().toList());

        effects.forEach(CustomEffectRegistry::register);
    }


}
