package dev.overgrown.apoli;

import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class PowerContainerAttachment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Apoli.MOD_ID);

    public static final Supplier<AttachmentType<PowerContainerImpl>> POWER_CONTAINER =
        ATTACHMENT_TYPES.register("power_container", () -> AttachmentType.builder(PowerContainerImpl::new)
            .serialize(PowerContainerImpl.CODEC)
            .copyOnDeath()
            .build());

    private PowerContainerAttachment() {}

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }

    private static java.util.function.Function<Entity, PowerContainer> CLIENT_LOOKUP = e -> null;

    public static void setClientLookup(java.util.function.Function<Entity, PowerContainer> lookup) {
        CLIENT_LOOKUP = lookup;
    }

    public static @Nullable PowerContainer get(Entity entity) {
        if (entity.level().isClientSide()) {
            return CLIENT_LOOKUP.apply(entity);
        }
        if (!entity.hasData(POWER_CONTAINER.get())) return null;
        PowerContainerImpl impl = entity.getData(POWER_CONTAINER.get());
        impl.attachOwner(entity);
        return impl;
    }

    public static @Nullable PowerContainer getOrCreate(Entity entity) {
        PowerContainerImpl impl = entity.getData(POWER_CONTAINER.get());
        impl.attachOwner(entity);
        return impl;
    }
}
