package dev.overgrown.apoli.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PowerContainerImpl implements PowerContainer {
    private final Map<ResourceLocation, Set<ResourceLocation>> bySources = new HashMap<>();
    private final Map<ResourceLocation, Integer> auxInt = new HashMap<>();
    private final Map<ResourceLocation, CompoundTag> auxNbt = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> suppressedBySources = new HashMap<>();
    private @Nullable Entity owner;
    private boolean dirty;
    private boolean structureDirty;

    public PowerContainerImpl() {}

    public java.util.OptionalInt getAuxInt(ResourceLocation powerId) {
        Integer v = auxInt.get(powerId);
        return v == null ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(v);
    }

    public void setAuxInt(ResourceLocation powerId, int value) {
        Integer prev = auxInt.put(powerId, value);
        if (prev == null || prev != value) markDirty();
    }

    public void removeAux(ResourceLocation powerId) {
        boolean changed = auxInt.remove(powerId) != null;
        if (auxNbt.remove(powerId) != null) changed = true;
        if (changed) markDirty();
    }

    public Map<ResourceLocation, Integer> auxIntSnapshot() {
        return Map.copyOf(auxInt);
    }

    public @Nullable CompoundTag getAuxNbt(ResourceLocation powerId) {
        return auxNbt.get(powerId);
    }

    public void setAuxNbt(ResourceLocation powerId, CompoundTag tag) {
        auxNbt.put(powerId, tag);
    }

    public void attachOwner(Entity entity) {
        this.owner = entity;
        if (entity != null && !bySources.isEmpty()) PoweredEntities.register(entity);
    }

    @Override
    public boolean addPower(ResourceLocation power, ResourceLocation source) {
        boolean wasEmpty = bySources.isEmpty();
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
        if (added) markStructureDirty();
        if (wasEmpty && !bySources.isEmpty()) PoweredEntities.register(owner);
        return added;
    }

    @Override
    public boolean removePower(ResourceLocation power, ResourceLocation source) {
        Set<ResourceLocation> sources = bySources.get(power);
        if (sources == null) return false;
        boolean removed = sources.remove(source);
        if (sources.isEmpty()) {
            bySources.remove(power);
            suppressedBySources.remove(power);
            releaseSuppressionSource(power);
            if (removed && owner != null && owner.level() instanceof ServerLevel) {
                Power loaded = ApoliPowers.get(power);
                if (loaded != null) {
                    PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
                    if (type != null) invokeOnRemoved(type, power, loaded.config(), source);
                }
            }
            if (auxInt.remove(power) != null) markDirty();
        }
        if (removed) markStructureDirty();
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
        suppressedBySources.remove(power);
        releaseSuppressionSource(power);
        if (auxInt.remove(power) != null) markDirty();
        markStructureDirty();
        return true;
    }

    @Override
    public void clear() {
        if (bySources.isEmpty()) return;
        bySources.clear();
        suppressedBySources.clear();
        if (!auxInt.isEmpty()) {
            auxInt.clear();
            markDirty();
        }
        markStructureDirty();
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
    public boolean suppressPower(ResourceLocation power, ResourceLocation source) {
        Set<ResourceLocation> sources = suppressedBySources.computeIfAbsent(power, k -> new HashSet<>());
        boolean wasSuppressed = !sources.isEmpty();
        boolean added = sources.add(source);
        if (added && !wasSuppressed) markStructureDirty();
        return added;
    }

    @Override
    public boolean unsuppressPower(ResourceLocation power, ResourceLocation source) {
        Set<ResourceLocation> sources = suppressedBySources.get(power);
        if (sources == null) return false;
        boolean removed = sources.remove(source);
        if (sources.isEmpty()) suppressedBySources.remove(power);
        if (removed && !isSuppressed(power)) markStructureDirty();
        return removed;
    }

    @Override
    public boolean isSuppressed(ResourceLocation power) {
        return suppressedBySources.containsKey(power);
    }

    @Override
    public Set<ResourceLocation> suppressedPowers() {
        return Set.copyOf(suppressedBySources.keySet());
    }

    private void releaseSuppressionSource(ResourceLocation source) {
        boolean changed = false;
        for (var it = suppressedBySources.values().iterator(); it.hasNext(); ) {
            Set<ResourceLocation> set = it.next();
            if (set.remove(source)) {
                changed = true;
                if (set.isEmpty()) it.remove();
            }
        }
        if (changed) markStructureDirty();
    }

    @Override
    public LivingEntity owner() {
        if (owner == null) throw new IllegalStateException("PowerContainer not attached to an entity");
        return owner instanceof LivingEntity le ? le : null;
    }

    @Override
    public Entity rawOwner() {
        if (owner == null) throw new IllegalStateException("PowerContainer not attached to an entity");
        return owner;
    }

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    private void markStructureDirty() {
        this.structureDirty = true;
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    public boolean isStructureDirty() {
        return structureDirty;
    }

    public void clearStructureDirty() {
        this.structureDirty = false;
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
        EntityCtx ctx = EntityCtx.of(owner, level);
        for (ResourceLocation powerId : List.copyOf(bySources.keySet())) {
            if (!bySources.containsKey(powerId)) continue;
            if (isSuppressed(powerId)) continue;
            Power loaded = ApoliPowers.get(powerId);
            if (loaded == null) continue;
            PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
            if (type == null) continue;
            if (!(owner instanceof LivingEntity) && !type.ticksNonLivingEntities()) continue;
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
            }),
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
            .optionalFieldOf("aux_int", Map.of())
            .forGetter(c -> Map.copyOf(c.auxInt)),
        Codec.unboundedMap(ResourceLocation.CODEC, CompoundTag.CODEC)
            .optionalFieldOf("aux_nbt", Map.of())
            .forGetter(c -> Map.copyOf(c.auxNbt)),
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.list(ResourceLocation.CODEC))
            .optionalFieldOf("suppressed", Map.of())
            .forGetter(c -> {
                Map<ResourceLocation, List<ResourceLocation>> out = new HashMap<>(c.suppressedBySources.size());
                c.suppressedBySources.forEach((k, v) -> out.put(k, List.copyOf(v)));
                return out;
            })
    ).apply(instance, (powers, aux, nbt, suppressed) -> {
        PowerContainerImpl c = new PowerContainerImpl();
        powers.forEach((k, v) -> c.bySources.put(k, new HashSet<>(v)));
        c.auxInt.putAll(aux);
        c.auxNbt.putAll(nbt);
        suppressed.forEach((k, v) -> c.suppressedBySources.put(k, new HashSet<>(v)));
        return c;
    }));
}
