package dev.overgrown.apoli.client;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.effects.CustomEffect;
import dev.overgrown.apoli.effects.CustomEffectNetworking;
import dev.overgrown.apoli.effects.CustomEffectRegistry;
import dev.overgrown.apoli.effects.RuntimeMobEffectRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class SyncCustomEffectRegistry {
    public static void sync(FriendlyByteBuf buf, PacketSender sender) {
        var packet = new CustomEffectNetworking.SyncCustomEffectsPacket(buf);
        Apoli.LOGGER.debug("Received Payload for {} Effects.", packet.effects().size());

        List<CustomEffect> effects = new java.util.ArrayList<>(List.of());

        packet.effects().forEach(effect -> effects.add(CustomEffect.create(effect.id(), List.of(), Vec3.fromRGB24(effect.color()).scale(((double) 1 /255)), effect.icon(), 0, MobEffectCategory.NEUTRAL, effect.name())));

        ((RuntimeMobEffectRegistry) BuiltInRegistries.MOB_EFFECT).apoli$truncate(effects.stream().sorted().toList());
        effects.forEach(CustomEffectRegistry::register);

        var success = true;
        var responsePacket = new CustomEffectNetworking.CustomEffectResponsePacket(success);

        sender.sendPacket(responsePacket);
        Apoli.LOGGER.debug("Send Response Packet with success = {}.", responsePacket.success());
    }
}
