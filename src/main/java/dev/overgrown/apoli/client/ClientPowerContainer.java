package dev.overgrown.apoli.client;

import dev.overgrown.apoli.power.PowerContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class ClientPowerContainer implements PowerContainer {
    private final Entity entity;

    public ClientPowerContainer(Entity entity) {
        this.entity = entity;
    }

    @Override public boolean addPower(ResourceLocation power, ResourceLocation source) { return false; }
    @Override public boolean removePower(ResourceLocation power, ResourceLocation source) { return false; }
    @Override public boolean removeAllFromSource(ResourceLocation source) { return false; }
    @Override public boolean removePowerCompletely(ResourceLocation power) { return false; }
    @Override public void clear() {}
    @Override public void markDirty() {}

    @Override public boolean hasPower(ResourceLocation power) {
        return ClientPowerState.powersFor(entity.getId()).containsKey(power);
    }

    @Override public Set<ResourceLocation> sourcesOf(ResourceLocation power) {
        Set<ResourceLocation> s = ClientPowerState.powersFor(entity.getId()).get(power);
        return s == null ? Set.of() : s;
    }

    @Override public boolean isEmpty() {
        return ClientPowerState.powersFor(entity.getId()).isEmpty();
    }

    @Override public Set<ResourceLocation> allPowers() {
        return ClientPowerState.powersFor(entity.getId()).keySet();
    }

    @Override public Set<ResourceLocation> allSources() {
        HashSet<ResourceLocation> out = new HashSet<>();
        for (Set<ResourceLocation> s : ClientPowerState.powersFor(entity.getId()).values()) out.addAll(s);
        return out;
    }

    @Override public LivingEntity owner() {
        return entity instanceof LivingEntity le ? le : null;
    }

    @Override public Entity rawOwner() {
        return entity;
    }

    @Override public OptionalInt getAuxInt(ResourceLocation powerId) {
        Integer v = ClientPowerState.auxFor(entity.getId()).get(powerId);
        return v == null ? OptionalInt.empty() : OptionalInt.of(v);
    }

    @Override public int getAuxIntOr(ResourceLocation powerId, int fallback) {
        Integer v = ClientPowerState.auxFor(entity.getId()).get(powerId);
        return v == null ? fallback : v;
    }

    @Override public boolean suppressPower(ResourceLocation power, ResourceLocation source) { return false; }
    @Override public boolean unsuppressPower(ResourceLocation power, ResourceLocation source) { return false; }

    @Override public boolean isSuppressed(ResourceLocation power) {
        return ClientPowerState.suppressedFor(entity.getId()).contains(power);
    }

    @Override public Set<ResourceLocation> suppressedPowers() {
        return ClientPowerState.suppressedFor(entity.getId());
    }
}
