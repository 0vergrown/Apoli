package dev.overgrown.apoli.power;

import dev.overgrown.apoli.PowerContainerAttachment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

public interface PowerContainer {
    boolean addPower(ResourceLocation power, ResourceLocation source);

    boolean removePower(ResourceLocation power, ResourceLocation source);

    boolean removeAllFromSource(ResourceLocation source);

    boolean removePowerCompletely(ResourceLocation power);

    void clear();

    boolean hasPower(ResourceLocation power);

    default boolean isEmpty() {
        return allPowers().isEmpty();
    }

    default List<ResourceLocation> powersOfType(ResourceLocation canonicalTypeId) {
        List<ResourceLocation> out = null;
        for (ResourceLocation powerId : allPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!canonicalTypeId.equals(PowerTypeRegistry.resolveId(power.typeId()))) continue;
            if (out == null) out = new ArrayList<>(2);
            out.add(powerId);
        }
        return out == null ? List.of() : out;
    }

    Set<ResourceLocation> sourcesOf(ResourceLocation power);

    Set<ResourceLocation> allPowers();

    Set<ResourceLocation> allSources();

    boolean suppressPower(ResourceLocation power, ResourceLocation source);

    boolean unsuppressPower(ResourceLocation power, ResourceLocation source);

    boolean isSuppressed(ResourceLocation power);

    Set<ResourceLocation> suppressedPowers();

    Set<ResourceLocation> suppressionSourcesOf(ResourceLocation power);

    LivingEntity owner();

    Entity rawOwner();

    void markDirty();

    OptionalInt getAuxInt(ResourceLocation powerId);

    default int getAuxIntOr(ResourceLocation powerId, int fallback) {
        OptionalInt v = getAuxInt(powerId);
        return v.isPresent() ? v.getAsInt() : fallback;
    }

    static @Nullable PowerContainer of(Entity entity) {
        return PowerContainerAttachment.get(entity);
    }
}
