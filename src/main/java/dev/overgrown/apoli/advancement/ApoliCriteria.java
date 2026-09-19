package dev.overgrown.apoli.advancement;

import dev.overgrown.apoli.Apoli;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class ApoliCriteria {

    public static ResourceTrigger RESOURCE;
    public static PowerTrigger POWER;

    private ApoliCriteria() {}

    public static void register() {
        RESOURCE = Registry.register(BuiltInRegistries.TRIGGER_TYPES, Apoli.id("resource"), new ResourceTrigger());
        POWER = Registry.register(BuiltInRegistries.TRIGGER_TYPES, Apoli.id("power"), new PowerTrigger());
    }

    public static void resourceChanged(@Nullable Entity owner, ResourceLocation resource, int value) {
        if (RESOURCE != null && owner instanceof ServerPlayer player) RESOURCE.trigger(player, resource, value);
    }

    public static void powerGranted(@Nullable Entity owner, ResourceLocation power, ResourceLocation source) {
        if (POWER != null && owner instanceof ServerPlayer player) POWER.trigger(player, power, source);
    }
}
