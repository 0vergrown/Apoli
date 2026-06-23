package dev.overgrown.apoli;

import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class PowerContainerAttachment {
    public static final AttachmentType<PowerContainerImpl> TYPE = AttachmentRegistry.<PowerContainerImpl>builder()
        .initializer(PowerContainerImpl::new)
        .persistent(PowerContainerImpl.CODEC)
        .copyOnDeath()
        .buildAndRegister(Apoli.id("power_container"));

    private PowerContainerAttachment() {}

    private static java.util.function.Function<Entity, PowerContainer> CLIENT_LOOKUP = e -> null;

    public static void setClientLookup(java.util.function.Function<Entity, PowerContainer> lookup) {
        CLIENT_LOOKUP = lookup;
    }

    public static @Nullable PowerContainer get(Entity entity) {
        if (entity.level().isClientSide()) {
            PowerContainer client = CLIENT_LOOKUP.apply(entity);
            if (client != null) return client;
        }
        PowerContainerImpl impl = entity.getAttached(TYPE);
        if (impl == null) return null;
        impl.attachOwner(entity);
        return impl;
    }

    public static @Nullable PowerContainer getOrCreate(Entity entity) {
        PowerContainerImpl impl = entity.getAttachedOrCreate(TYPE);
        impl.attachOwner(entity);
        return impl;
    }
}
