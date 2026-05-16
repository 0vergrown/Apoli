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

    public static @Nullable PowerContainer get(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return null;
        PowerContainerImpl impl = living.getAttached(TYPE);
        if (impl == null) return null;
        impl.attachOwner(living);
        return impl;
    }

    public static @Nullable PowerContainer getOrCreate(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return null;
        PowerContainerImpl impl = living.getAttachedOrCreate(TYPE);
        impl.attachOwner(living);
        return impl;
    }
}
