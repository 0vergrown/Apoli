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

/**
 * NeoForge's native typed attachment system replaces the Capability boilerplate
 * required on legacy Forge. Persistence + copy-on-death are declared on the
 * builder; everything else (NBT round-trip, slot allocation, save/load hooks)
 * is handled by the engine.
 */
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

    public static @Nullable PowerContainer get(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return null;
        PowerContainerImpl impl = living.getData(POWER_CONTAINER.get());
        impl.attachOwner(living);
        return impl;
    }

    public static @Nullable PowerContainer getOrCreate(Entity entity) {
        // The default initializer means getData always returns a real instance;
        // there is no separate "create" call to make.
        return get(entity);
    }
}
