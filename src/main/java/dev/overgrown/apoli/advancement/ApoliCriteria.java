package dev.overgrown.apoli.advancement;

import dev.overgrown.apoli.Apoli;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class ApoliCriteria {

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
        DeferredRegister.create(Registries.TRIGGER_TYPE, Apoli.MOD_ID);

    public static final Supplier<ResourceTrigger> RESOURCE =
        TRIGGERS.register("resource", ResourceTrigger::new);

    public static final Supplier<PowerTrigger> POWER =
        TRIGGERS.register("power", PowerTrigger::new);

    private ApoliCriteria() {}

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
    }

    public static void resourceChanged(@Nullable Entity owner, ResourceLocation resource, int value) {
        if (owner instanceof ServerPlayer player) RESOURCE.get().trigger(player, resource, value);
    }

    public static void powerGranted(@Nullable Entity owner, ResourceLocation power, ResourceLocation source) {
        if (owner instanceof ServerPlayer player) POWER.get().trigger(player, power, source);
    }
}
