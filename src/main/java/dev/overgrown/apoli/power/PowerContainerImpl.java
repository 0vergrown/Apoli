package dev.overgrown.apoli.power;

import dev.overgrown.apoli.attribution.PowerCause;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.context.EntityCtx;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMaps;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PowerContainerImpl implements PowerContainer {
    private final Map<ResourceLocation, Set<ResourceLocation>> bySources = new HashMap<>();
    private final Object2DoubleOpenHashMap<ResourceLocation> auxValues = newAuxMap();
    private final Map<ResourceLocation, CompoundTag> auxNbt = new HashMap<>();
    private final Map<ResourceLocation, double[]> auxTables = new HashMap<>();
    private final Map<ResourceLocation, int[]> scratchInts = new HashMap<>();
    private final Map<ResourceLocation, Set<ResourceLocation>> suppressedBySources = new HashMap<>();
    private @Nullable Entity owner;
    private boolean dirty;
    private boolean structureDirty;

    private @Nullable CompoundTag cachedSaveTag;
    private @Nullable Map<ResourceLocation, List<ResourceLocation>> typeIndex;
    private long @Nullable [] typeBits;
    private @Nullable Map<ResourceLocation, List<ResourceLocation>> typeBitsSource;
    private @Nullable List<TickEntry> tickList;
    private @Nullable Set<ResourceLocation> effectiveSuppressed;
    private Set<ResourceLocation> notifiedSuppressed = Set.of();
    private int cacheGeneration = -1;
    private int reconciledGeneration = -1;

    private record TickEntry(ResourceLocation powerId, Power power, PowerType<?> type) {}

    public PowerContainerImpl() {}

    private static Object2DoubleOpenHashMap<ResourceLocation> newAuxMap() {
        Object2DoubleOpenHashMap<ResourceLocation> map = new Object2DoubleOpenHashMap<>();
        map.defaultReturnValue(Double.NaN);
        return map;
    }

    @Override
    public double getAuxDoubleOr(ResourceLocation powerId, double fallback) {
        double value = auxValues.getDouble(powerId);
        return Double.isNaN(value) ? fallback : value;
    }

    public void setAuxDouble(ResourceLocation powerId, double value) {
        double stored = Double.isFinite(value) ? value : 0.0;
        double previous = auxValues.put(powerId, stored);
        if (Double.isNaN(previous) || previous != stored) markDirty();
    }

    public void setAuxInt(ResourceLocation powerId, int value) {
        setAuxDouble(powerId, value);
    }

    @Override
    public double @Nullable [] getAuxDoubles(ResourceLocation powerId) {
        return auxTables.get(powerId);
    }

    public void setAuxDoubles(ResourceLocation powerId, double[] values) {
        double[] previous = auxTables.put(powerId, values);
        if (previous == null || !java.util.Arrays.equals(previous, values)) markDirty();
    }

    public double[] auxDoublesAtLeast(ResourceLocation powerId, int length, double fill) {
        double[] existing = auxTables.get(powerId);
        if (existing != null && existing.length >= length) return existing;
        double[] grown = new double[length];
        if (fill != 0.0) java.util.Arrays.fill(grown, fill);
        if (existing != null) System.arraycopy(existing, 0, grown, 0, existing.length);
        auxTables.put(powerId, grown);
        markDirty();
        return grown;
    }

    public void trimAuxDoubles(ResourceLocation powerId, int maxLength) {
        double[] existing = auxTables.get(powerId);
        if (existing == null || existing.length <= maxLength) return;
        if (maxLength <= 0) {
            auxTables.remove(powerId);
        } else {
            auxTables.put(powerId, java.util.Arrays.copyOf(existing, maxLength));
        }
        markDirty();
    }

    public int[] scratchInts(ResourceLocation powerId, int length) {
        int[] existing = scratchInts.get(powerId);
        if (existing != null && existing.length >= length) return existing;
        int[] grown = new int[length];
        if (existing != null) System.arraycopy(existing, 0, grown, 0, existing.length);
        scratchInts.put(powerId, grown);
        return grown;
    }

    public Map<ResourceLocation, double[]> auxTablesSnapshot() {
        return auxTables.isEmpty() ? Map.of() : new HashMap<>(auxTables);
    }

    public void removeAux(ResourceLocation powerId) {
        scratchInts.remove(powerId);
        boolean changed = !Double.isNaN(auxValues.removeDouble(powerId));
        if (auxTables.remove(powerId) != null) changed = true;
        if (auxNbt.remove(powerId) != null) {
            changed = true;
            if (this.owner instanceof net.minecraft.server.level.ServerPlayer player) {
                dev.overgrown.apoli.ApoliNetwork.sendPowerInventory(player,
                    new dev.overgrown.apoli.network.payload.PowerInventoryS2C(powerId, null));
            }
        }
        if (changed) markDirty();
    }

    public Object2DoubleMap<ResourceLocation> auxSnapshot() {
        if (auxValues.isEmpty()) return Object2DoubleMaps.emptyMap();
        Object2DoubleOpenHashMap<ResourceLocation> copy = new Object2DoubleOpenHashMap<>(auxValues);
        copy.defaultReturnValue(Double.NaN);
        return copy;
    }

    public @Nullable CompoundTag getAuxNbt(ResourceLocation powerId) {
        return auxNbt.get(powerId);
    }

    public void setAuxNbt(ResourceLocation powerId, CompoundTag tag) {
        auxNbt.put(powerId, tag);
        this.cachedSaveTag = null;
        if (this.owner instanceof net.minecraft.server.level.ServerPlayer player) {
            dev.overgrown.apoli.ApoliNetwork.sendPowerInventory(player,
                new dev.overgrown.apoli.network.payload.PowerInventoryS2C(powerId, tag));
        }
    }

    public void attachOwner(Entity entity) {
        if (this.owner == entity) return;
        this.owner = entity;
        if (entity != null && !bySources.isEmpty()) PoweredEntities.register(entity);
    }

    @Override
    public boolean isEmpty() {
        return bySources.isEmpty();
    }

    @Override
    public boolean addPower(ResourceLocation power, ResourceLocation source) {
        boolean wasEmpty = bySources.isEmpty();
        Set<ResourceLocation> sources = bySources.computeIfAbsent(power, k -> new HashSet<>());
        boolean first = sources.isEmpty();
        boolean added = sources.add(source);
        if (added) markStructureDirty();
        if (added && first && owner != null && owner.level() instanceof ServerLevel) {
            Power typed = ApoliPowers.get(power);
            if (typed != null) PowerTypeUsage.markHeld(PowerTypeRegistry.resolveId(typed.typeId()));
        }
        if (added && first && owner != null && owner.level() instanceof ServerLevel) {
            Power loaded = ApoliPowers.get(power);
            if (loaded != null) {
                PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
                if (type != null) invokeOnAdded(type, power, loaded.config(), source);
            }
        }
        if (added) {
            refreshSuppression();
            dev.overgrown.apoli.advancement.ApoliCriteria.powerGranted(owner, power, source);
        }
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
            if (removed) markStructureDirty();
            if (removed && owner != null && owner.level() instanceof ServerLevel) {
                Power loaded = ApoliPowers.get(power);
                if (loaded != null) {
                    PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
                    if (type != null) invokeOnRemoved(type, power, loaded.config(), source);
                } else if (owner instanceof LivingEntity living) {
                    dev.overgrown.apoli.power.builtin.AttributePower.purge(living, power);
                }
                removeAllFromSource(power);
            }
            removeAux(power);
        }
        if (removed) {
            markStructureDirty();
            refreshSuppression();
        }
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
        markStructureDirty();
        if (owner != null && owner.level() instanceof ServerLevel) {
            Power loaded = ApoliPowers.get(power);
            if (loaded != null) {
                PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
                if (type != null) {
                    for (ResourceLocation source : sources) invokeOnRemoved(type, power, loaded.config(), source);
                }
            }
        }
        removeAux(power);
        removeAllFromSource(power);
        refreshSuppression();
        return true;
    }

    @Override
    public void clear() {
        if (bySources.isEmpty()) return;
        bySources.clear();
        suppressedBySources.clear();
        notifiedSuppressed = Set.of();
        if (!auxValues.isEmpty() || !auxTables.isEmpty() || !auxNbt.isEmpty()) {
            auxValues.clear();
            auxTables.clear();
            auxNbt.clear();
            scratchInts.clear();
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
        boolean added = sources.add(source);
        if (added) {
            markStructureDirty();
            refreshSuppression();
        }
        return added;
    }

    @Override
    public boolean unsuppressPower(ResourceLocation power, ResourceLocation source) {
        Set<ResourceLocation> sources = suppressedBySources.get(power);
        if (sources == null) return false;
        boolean removed = sources.remove(source);
        if (sources.isEmpty()) suppressedBySources.remove(power);
        if (removed) {
            markStructureDirty();
            refreshSuppression();
        }
        return removed;
    }

    @Override
    public boolean suppressAll(Collection<ResourceLocation> powers, ResourceLocation source) {
        boolean any = false;
        for (ResourceLocation power : powers) {
            if (suppressedBySources.computeIfAbsent(power, k -> new HashSet<>()).add(source)) any = true;
        }
        if (any) {
            markStructureDirty();
            refreshSuppression();
        }
        return any;
    }

    @Override
    public boolean unsuppressAll(Collection<ResourceLocation> powers, ResourceLocation source) {
        boolean any = false;
        for (ResourceLocation power : powers) {
            Set<ResourceLocation> sources = suppressedBySources.get(power);
            if (sources == null) continue;
            if (sources.remove(source)) any = true;
            if (sources.isEmpty()) suppressedBySources.remove(power);
        }
        if (any) {
            markStructureDirty();
            refreshSuppression();
        }
        return any;
    }

    @Override
    public boolean isSuppressed(ResourceLocation power) {
        if (suppressedBySources.isEmpty()) return false;
        return effectiveSuppressed().contains(power);
    }

    @Override
    public Set<ResourceLocation> suppressedPowers() {
        return effectiveSuppressed();
    }

    @Override
    public Set<ResourceLocation> directlySuppressedPowers() {
        return Set.copyOf(suppressedBySources.keySet());
    }

    @Override
    public Set<ResourceLocation> suppressionSourcesOf(ResourceLocation power) {
        Set<ResourceLocation> sources = suppressedBySources.get(power);
        return sources == null ? Set.of() : Set.copyOf(sources);
    }

    private Set<ResourceLocation> effectiveSuppressed() {
        Set<ResourceLocation> cached = this.effectiveSuppressed;
        if (cached == null) {
            cached = computeEffectiveSuppressed();
            this.effectiveSuppressed = cached;
        }
        return cached;
    }

    private Set<ResourceLocation> computeEffectiveSuppressed() {
        if (suppressedBySources.isEmpty()) return Set.of();
        Set<ResourceLocation> out = new HashSet<>(suppressedBySources.keySet());
        boolean grew = !bySources.isEmpty();
        while (grew) {
            grew = false;
            for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : bySources.entrySet()) {
                if (out.contains(entry.getKey())) continue;
                Set<ResourceLocation> grantedBy = entry.getValue();
                if (grantedBy.isEmpty()) continue;
                boolean everySourceSuppressed = true;
                for (ResourceLocation source : grantedBy) {
                    if (out.contains(source)) continue;
                    everySourceSuppressed = false;
                    break;
                }
                if (!everySourceSuppressed) continue;
                out.add(entry.getKey());
                grew = true;
            }
        }
        return Set.copyOf(out);
    }

    private void refreshSuppression() {
        if (suppressedBySources.isEmpty() && notifiedSuppressed.isEmpty()) return;
        Set<ResourceLocation> now = effectiveSuppressed();
        if (now.equals(notifiedSuppressed)) return;
        Set<ResourceLocation> before = notifiedSuppressed;
        notifiedSuppressed = now;
        if (owner == null || !(owner.level() instanceof ServerLevel)) return;
        for (ResourceLocation powerId : now) {
            if (!before.contains(powerId)) fireSuppression(powerId, true);
        }
        for (ResourceLocation powerId : before) {
            if (!now.contains(powerId)) fireSuppression(powerId, false);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fireSuppression(ResourceLocation powerId, boolean suppressed) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return;
        PowerType type = PowerTypeRegistry.get(loaded.typeId());
        if (type == null) return;
        if (suppressed) type.onSuppressed(powerId, loaded.config(), this);
        else type.onUnsuppressed(powerId, loaded.config(), this);
    }

    @Override
    public boolean unsuppressAllFromSource(ResourceLocation source) {
        return releaseSuppressionSource(source);
    }

    private boolean releaseSuppressionSource(ResourceLocation source) {
        boolean changed = false;
        for (var it = suppressedBySources.values().iterator(); it.hasNext(); ) {
            Set<ResourceLocation> set = it.next();
            if (set.remove(source)) {
                changed = true;
                if (set.isEmpty()) it.remove();
            }
        }
        if (changed) {
            markStructureDirty();
            refreshSuppression();
        }
        return changed;
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
        this.cachedSaveTag = null;
    }

    private void markStructureDirty() {
        this.structureDirty = true;
        this.dirty = true;
        this.cachedSaveTag = null;
        this.typeIndex = null;
        this.tickList = null;
        this.effectiveSuppressed = null;
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
        this.cachedSaveTag = null;
        this.typeIndex = null;
        this.tickList = null;
        this.effectiveSuppressed = null;
        this.notifiedSuppressed = computeEffectiveSuppressed();
    }

    private void ensureCacheGeneration() {
        int gen = ApoliPowers.generation();
        if (cacheGeneration != gen) {
            cacheGeneration = gen;
            typeIndex = null;
            tickList = null;
        }
    }

    @Override
    public List<ResourceLocation> powersOfType(ResourceLocation canonicalTypeId) {
        if (bySources.isEmpty()) return List.of();
        List<ResourceLocation> list = typeIndex().get(canonicalTypeId);
        return list == null ? List.of() : list;
    }

    private Map<ResourceLocation, List<ResourceLocation>> typeIndex() {
        ensureCacheGeneration();
        Map<ResourceLocation, List<ResourceLocation>> index = this.typeIndex;
        if (index == null) {
            index = new HashMap<>();
            for (ResourceLocation powerId : bySources.keySet()) {
                Power power = ApoliPowers.get(powerId);
                if (power == null) continue;
                index.computeIfAbsent(PowerTypeRegistry.resolveId(power.typeId()), k -> new ArrayList<>(2))
                    .add(powerId);
            }
            this.typeIndex = index;
        }
        return index;
    }

    public long[] heldTypeBits() {
        if (bySources.isEmpty()) return EMPTY_BITS;
        Map<ResourceLocation, List<ResourceLocation>> index = typeIndex();
        long[] bits = this.typeBits;
        if (bits == null || this.typeBitsSource != index) {
            bits = PowerTypeUsage.bitsOf(index.keySet());
            this.typeBits = bits;
            this.typeBitsSource = index;
        }
        return bits;
    }

    private static final long[] EMPTY_BITS = new long[0];

    private List<TickEntry> tickEntries() {
        ensureCacheGeneration();
        List<TickEntry> list = this.tickList;
        if (list == null) {
            if (bySources.isEmpty()) {
                list = List.of();
            } else {
                Set<ResourceLocation> suppressed = effectiveSuppressed();
                List<TickEntry> building = new ArrayList<>(bySources.size());
                for (ResourceLocation powerId : bySources.keySet()) {
                    if (suppressed.contains(powerId)) continue;
                    Power power = ApoliPowers.get(powerId);
                    if (power == null) continue;
                    PowerType<?> type = PowerTypeRegistry.get(power.typeId());
                    if (type == null) continue;
                    building.add(new TickEntry(powerId, power, type));
                }
                list = List.copyOf(building);
            }
            this.tickList = list;
        }
        return list;
    }

    public void tickActive() {
        if (owner == null || !(owner.level() instanceof ServerLevel level)) return;
        int generation = ApoliPowers.generation();
        if (reconciledGeneration != generation) {
            reconciledGeneration = generation;
            dev.overgrown.apoli.power.builtin.MultiplePower.reconcile(this);
            dev.overgrown.apoli.power.builtin.PowerStoragePower.reconcile(this);
        }
        List<TickEntry> entries = tickEntries();
        if (entries.isEmpty()) return;
        EntityCtx ctx = EntityCtx.of(owner, level);
        boolean living = owner instanceof LivingEntity;

        for (TickEntry entry : entries) {
            if (!bySources.containsKey(entry.powerId())) continue;
            if (isSuppressed(entry.powerId())) continue;
            if (!living && !entry.type().ticksNonLivingEntities()) continue;
            if (!isActive(entry.type(), entry.powerId(), entry.power().config(), ctx)) continue;
            invokeTick(entry.type(), entry.powerId(), entry.power().config());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean isActive(PowerType type, ResourceLocation powerId, Object cfg, EntityCtx ctx) {
        return type.isActive(powerId, cfg, ctx);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void invokeTick(PowerType type, ResourceLocation powerId, Object cfg) {
        boolean attributed = PowerCause.push(owner, powerId);
        try {
            type.tick(powerId, cfg, this);
        } finally {
            if (attributed) PowerCause.pop();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void invokeOnAdded(PowerType type, ResourceLocation powerId, Object cfg, ResourceLocation source) {
        type.onAdded(powerId, cfg, this, source);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void invokeOnRemoved(PowerType type, ResourceLocation powerId, Object cfg, ResourceLocation source) {
        type.onRemoved(powerId, cfg, this, source);
    }

    private CompoundTag saveTag() {
        CompoundTag tag = this.cachedSaveTag;
        if (tag == null) {
            tag = buildSaveTag();
            this.cachedSaveTag = tag;
        }
        return tag;
    }

    private CompoundTag buildSaveTag() {
        CompoundTag root = new CompoundTag();
        root.put("powers", idListsToTag(bySources));
        if (!auxValues.isEmpty()) {
            CompoundTag aux = new CompoundTag();
            for (Object2DoubleMap.Entry<ResourceLocation> entry : Object2DoubleMaps.fastIterable(auxValues)) {
                aux.put(entry.getKey().toString(), numberTag(entry.getDoubleValue()));
            }
            root.put("aux_int", aux);
        }
        if (!auxNbt.isEmpty()) {
            CompoundTag aux = new CompoundTag();
            auxNbt.forEach((id, stored) -> aux.put(id.toString(), stored.copy()));
            root.put("aux_nbt", aux);
        }
        if (!auxTables.isEmpty()) {
            CompoundTag aux = new CompoundTag();
            auxTables.forEach((id, values) -> aux.put(id.toString(), tableTag(values)));
            root.put("aux_ints", aux);
        }
        if (!suppressedBySources.isEmpty()) {
            root.put("suppressed", idListsToTag(suppressedBySources));
        }
        return root;
    }

    private static boolean storesAsInt(double value) {
        return value == Math.rint(value) && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    private static Tag numberTag(double value) {
        return storesAsInt(value) ? IntTag.valueOf((int) value) : DoubleTag.valueOf(value);
    }

    private static Tag tableTag(double[] values) {
        boolean whole = true;
        for (double value : values) {
            if (storesAsInt(value)) continue;
            whole = false;
            break;
        }
        if (whole) {
            int[] ints = new int[values.length];
            for (int i = 0; i < values.length; i++) ints[i] = (int) values[i];
            return new IntArrayTag(ints);
        }
        ListTag list = new ListTag();
        for (double value : values) list.add(DoubleTag.valueOf(value));
        return list;
    }

    private static final Codec<Double> AUX_NUMBER = Codec.of(
        new com.mojang.serialization.Encoder<Double>() {
            @Override
            public <T> DataResult<T> encode(Double input, DynamicOps<T> ops, T prefix) {
                double value = input;
                T number = storesAsInt(value) ? ops.createInt((int) value) : ops.createDouble(value);
                return ops.mergeToPrimitive(prefix, number);
            }
        },
        Codec.DOUBLE);

    private static CompoundTag idListsToTag(Map<ResourceLocation, Set<ResourceLocation>> map) {
        CompoundTag out = new CompoundTag();
        map.forEach((id, set) -> {
            ListTag list = new ListTag();
            for (ResourceLocation entry : set) list.add(StringTag.valueOf(entry.toString()));
            out.put(id.toString(), list);
        });
        return out;
    }

    private static final Codec<PowerContainerImpl> FULL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.list(ResourceLocation.CODEC))
            .fieldOf("powers")
            .forGetter(c -> {
                Map<ResourceLocation, List<ResourceLocation>> out = new HashMap<>(c.bySources.size());
                c.bySources.forEach((k, v) -> out.put(k, List.copyOf(v)));
                return out;
            }),
        Codec.unboundedMap(ResourceLocation.CODEC, AUX_NUMBER)
            .optionalFieldOf("aux_int", Map.of())
            .forGetter(c -> {
                if (c.auxValues.isEmpty()) return Map.of();
                Map<ResourceLocation, Double> out = new HashMap<>(c.auxValues.size());
                for (Object2DoubleMap.Entry<ResourceLocation> entry : Object2DoubleMaps.fastIterable(c.auxValues)) {
                    out.put(entry.getKey(), entry.getDoubleValue());
                }
                return out;
            }),
        Codec.unboundedMap(ResourceLocation.CODEC, CompoundTag.CODEC)
            .optionalFieldOf("aux_nbt", Map.of())
            .forGetter(c -> Map.copyOf(c.auxNbt)),
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.list(ResourceLocation.CODEC))
            .optionalFieldOf("suppressed", Map.of())
            .forGetter(c -> {
                Map<ResourceLocation, List<ResourceLocation>> out = new HashMap<>(c.suppressedBySources.size());
                c.suppressedBySources.forEach((k, v) -> out.put(k, List.copyOf(v)));
                return out;
            }),
        Codec.unboundedMap(ResourceLocation.CODEC, AUX_NUMBER.listOf())
            .optionalFieldOf("aux_ints", Map.of())
            .forGetter(c -> {
                if (c.auxTables.isEmpty()) return Map.of();
                Map<ResourceLocation, List<Double>> out = new HashMap<>(c.auxTables.size());
                c.auxTables.forEach((k, v) -> {
                    List<Double> boxed = new ArrayList<>(v.length);
                    for (double value : v) boxed.add(value);
                    out.put(k, boxed);
                });
                return out;
            })
    ).apply(instance, (powers, aux, nbt, suppressed, tables) -> {
        PowerContainerImpl c = new PowerContainerImpl();
        powers.forEach((k, v) -> c.bySources.put(k, new HashSet<>(v)));
        aux.forEach((k, v) -> c.auxValues.put(k, Double.isFinite(v) ? v : 0.0));
        c.auxNbt.putAll(nbt);
        tables.forEach((k, v) -> {
            double[] values = new double[v.size()];
            for (int i = 0; i < values.length; i++) {
                double value = v.get(i);
                values[i] = Double.isFinite(value) ? value : 0.0;
            }
            c.auxTables.put(k, values);
        });
        suppressed.forEach((k, v) -> c.suppressedBySources.put(k, new HashSet<>(v)));
        c.notifiedSuppressed = c.computeEffectiveSuppressed();
        return c;
    }));

    public static final Codec<PowerContainerImpl> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<PowerContainerImpl, T>> decode(DynamicOps<T> ops, T input) {
            return FULL_CODEC.decode(ops, input);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> DataResult<T> encode(PowerContainerImpl container, DynamicOps<T> ops, T prefix) {

            if (ops.empty() instanceof EndTag && ops.empty().equals(prefix)) {
                return DataResult.success((T) container.saveTag().copy());
            }
            return FULL_CODEC.encode(container, ops, prefix);
        }
    };
}
