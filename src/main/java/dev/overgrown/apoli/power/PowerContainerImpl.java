package dev.overgrown.apoli.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PowerContainerImpl implements PowerContainer {
    private final Map<ResourceLocation, Set<ResourceLocation>> bySources = new HashMap<>();
    private @Nullable LivingEntity owner;
    private boolean dirty;

    public PowerContainerImpl() {}

    public void attachOwner(LivingEntity entity) {
        this.owner = entity;
    }

    @Override
    public boolean addPower(ResourceLocation power, ResourceLocation source) {
        Set<ResourceLocation> sources = bySources.computeIfAbsent(power, k -> new HashSet<>());
        boolean first = sources.isEmpty();
        boolean added = sources.add(source);
        if (added && first && owner != null && owner.level() instanceof ServerLevel) {
            Power loaded = ApoliPowers.get(power);
            if (loaded != null) {
                PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
                if (type != null) invokeOnAdded(type, power, loaded.config(), source);
            }
        }
        if (added) markDirty();
        return added;
    }

    @Override
    public boolean removePower(ResourceLocation power, ResourceLocation source) {
        Set<ResourceLocation> sources = bySources.get(power);
        if (sources == null) return false;
        boolean removed = sources.remove(source);
        if (sources.isEmpty()) {
            bySources.remove(power);
            if (removed && owner != null && owner.level() instanceof ServerLevel) {
                Power loaded = ApoliPowers.get(power);
                if (loaded != null) {
                    PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
                    if (type != null) invokeOnRemoved(type, power, loaded.config(), source);
                }
            }
        }
        if (removed) markDirty();
        return removed;
    }

    @Override
    public boolean removeAllFromSource(ResourceLocation source) {
        boolean any = false;
        for (ResourceLocation power : List.copyOf(bySources.keySet())) {
            if (removePower(power, source)) any = true;
        }
        return any;
    }

    @Override
    public boolean removePowerCompletely(ResourceLocation power) {
        Set<ResourceLocation> sources = bySources.remove(power);
        if (sources == null) return false;
        markDirty();
        return true;
    }

    @Override
    public void clear() {
        if (bySources.isEmpty()) return;
        bySources.clear();
        markDirty();
    }

    @Override
    public boolean hasPower(ResourceLocation power) {
        Set<ResourceLocation> sources = bySources.get(power);
        return sources != null && !sources.isEmpty();
    }

    @Override
    public Set<ResourceLocation> sourcesOf(ResourceLocation power) {
        Set<ResourceLocation> sources = bySources.get(power);
        return sources == null ? Set.of() : Set.copyOf(sources);
    }

    @Override
    public Set<ResourceLocation> allPowers() {
        return Set.copyOf(bySources.keySet());
    }

    @Override
    public Set<ResourceLocation> allSources() {
        Set<ResourceLocation> all = new HashSet<>();
        for (Set<ResourceLocation> s : bySources.values()) all.addAll(s);
        return all;
    }

    @Override
    public LivingEntity owner() {
        if (owner == null) throw new IllegalStateException("PowerContainer not attached to an entity");
        return owner;
    }

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    public Map<ResourceLocation, Set<ResourceLocation>> snapshot() {
        Map<ResourceLocation, Set<ResourceLocation>> copy = new HashMap<>(bySources.size());
        bySources.forEach((k, v) -> copy.put(k, Set.copyOf(v)));
        return copy;
    }

    public void loadFromSnapshot(Map<ResourceLocation, Set<ResourceLocation>> snapshot) {
        bySources.clear();
        snapshot.forEach((k, v) -> bySources.put(k, new HashSet<>(v)));
    }

    public void tickActive() {
        if (owner == null || !(owner.level() instanceof ServerLevel level)) return;
        EntityCtx ctx = new EntityCtx(owner, level);
        for (var entry : bySources.entrySet()) {
            ResourceLocation powerId = entry.getKey();
            Power loaded = ApoliPowers.get(powerId);
            if (loaded == null) continue;
            PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
            if (type == null) continue;
            if (loaded.condition().isPresent() && !loaded.condition().get().test(ctx)) continue;
            if (!isActive(type, powerId, loaded.config(), ctx)) continue;
            invokeTick(type, powerId, loaded.config());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean isActive(PowerType type, ResourceLocation powerId, Object cfg, EntityCtx ctx) {
        return type.isActive(powerId, cfg, ctx);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void invokeTick(PowerType type, ResourceLocation powerId, Object cfg) {
        type.tick(powerId, cfg, this);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void invokeOnAdded(PowerType type, ResourceLocation powerId, Object cfg, ResourceLocation source) {
        type.onAdded(powerId, cfg, this, source);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void invokeOnRemoved(PowerType type, ResourceLocation powerId, Object cfg, ResourceLocation source) {
        type.onRemoved(powerId, cfg, this, source);
    }

    public static final Codec<PowerContainerImpl> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.list(ResourceLocation.CODEC))
            .fieldOf("powers")
            .forGetter(c -> {
                Map<ResourceLocation, List<ResourceLocation>> out = new HashMap<>(c.bySources.size());
                c.bySources.forEach((k, v) -> out.put(k, List.copyOf(v)));
                return out;
            })
    ).apply(instance, powers -> {
        PowerContainerImpl c = new PowerContainerImpl();
        powers.forEach((k, v) -> c.bySources.put(k, new HashSet<>(v)));
        return c;
    }));
}