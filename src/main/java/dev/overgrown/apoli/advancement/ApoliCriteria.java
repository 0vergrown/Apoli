package dev.overgrown.apoli.advancement;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class ApoliCriteria {

    public static ResourceTrigger RESOURCE;
    public static PowerTrigger POWER;

    private ApoliCriteria() {}

    public static void register() {
        RESOURCE = CriteriaTriggers.register(new ResourceTrigger());
        POWER = CriteriaTriggers.register(new PowerTrigger());
    }

    public static void resourceChanged(@Nullable Entity owner, ResourceLocation resource, int value) {
        if (RESOURCE != null && owner instanceof ServerPlayer player) RESOURCE.trigger(player, resource, value);
    }

    public static void powerGranted(@Nullable Entity owner, ResourceLocation power, ResourceLocation source) {
        if (POWER != null && owner instanceof ServerPlayer player) POWER.trigger(player, power, source);
    }
}
