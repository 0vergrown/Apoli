package dev.overgrown.apoli.power;

import dev.overgrown.apoli.PowerContainerAttachment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;
import java.util.Set;

public interface PowerContainer {
    boolean addPower(ResourceLocation power, ResourceLocation source);

    boolean removePower(ResourceLocation power, ResourceLocation source);

    boolean removeAllFromSource(ResourceLocation source);

    boolean removePowerCompletely(ResourceLocation power);

    void clear();

    boolean hasPower(ResourceLocation power);

    Set<ResourceLocation> sourcesOf(ResourceLocation power);

    Set<ResourceLocation> allPowers();

    Set<ResourceLocation> allSources();

    boolean suppressPower(ResourceLocation power, ResourceLocation source);

    boolean unsuppressPower(ResourceLocation power, ResourceLocation source);

    boolean isSuppressed(ResourceLocation power);

    Set<ResourceLocation> suppressedPowers();

    LivingEntity owner();

    Entity rawOwner();

    void markDirty();

    OptionalInt getAuxInt(ResourceLocation powerId);

    static @Nullable PowerContainer of(Entity entity) {
        return PowerContainerAttachment.get(entity);
    }
}
