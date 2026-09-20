package dev.overgrown.apoli.client;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.effects.CustomEffect;
import dev.overgrown.apoli.effects.CustomEffectNetworking;
import dev.overgrown.apoli.effects.CustomEffectRegistry;
import dev.overgrown.apoli.effects.RuntimeMobEffectRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SyncCustomEffectRegistry {
    public static void sync(CustomEffectNetworking.SyncCustomEffectsPayload payload, @Nullable ClientPlayNetworking.Context playCtx) {
        Apoli.LOGGER.debug("Received Payload for {} Effects.", payload.effects().size());

        List<CustomEffect> effects = new java.util.ArrayList<>(List.of());

        payload.effects().forEach(effect -> effects.add(CustomEffect.create(effect.id(), List.of(), Vec3.fromRGB24(effect.color()).scale(((double) 1 /255)), effect.icon(), 0, MobEffectCategory.NEUTRAL, effect.name())));

        ((RuntimeMobEffectRegistry) BuiltInRegistries.MOB_EFFECT).apoli$truncate(effects.stream().sorted().toList());

        effects.forEach(CustomEffectRegistry::register);

        if (playCtx != null) {
            var success = true;

            playCtx.responseSender().sendPacket(new CustomEffectNetworking.SyncCustomEffectsResponsePayload(success));

            Apoli.LOGGER.debug("Response Packet Send with success = {}", success);
        }
    }


}
