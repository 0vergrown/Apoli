package dev.overgrown.apoli.global;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GlobalPowers {

    public static final ResourceLocation SOURCE = Apoli.id("global");

    private static volatile List<GlobalPowerSet> SETS = List.of();
    private static volatile boolean ANY = false;

    private static final Map<EntityType<?>, List<ResourceLocation>> BY_TYPE = new ConcurrentHashMap<>();
    private static final List<ResourceLocation> NONE = List.of();

    private GlobalPowers() {}

    public static boolean any() {
        return ANY;
    }

    public static List<GlobalPowerSet> sets() {
        return SETS;
    }

    public static void replaceAll(Collection<GlobalPowerSet> loaded) {
        List<GlobalPowerSet> sorted = new ArrayList<>(loaded);
        sorted.sort(GlobalPowerSet::compareTo);
        SETS = List.copyOf(sorted);
        ANY = !SETS.isEmpty();
        BY_TYPE.clear();
    }

    public static List<ResourceLocation> powersFor(EntityType<?> entityType) {
        if (!ANY) return NONE;
        List<ResourceLocation> cached = BY_TYPE.get(entityType);
        if (cached != null) return cached;
        return BY_TYPE.computeIfAbsent(entityType, GlobalPowers::resolve);
    }

    private static List<ResourceLocation> resolve(EntityType<?> entityType) {
        Set<ResourceLocation> collected = null;
        for (GlobalPowerSet set : SETS) {
            if (!set.appliesTo(entityType)) continue;
            if (collected == null) collected = new LinkedHashSet<>(4);
            if (set.replace()) collected.clear();
            collected.addAll(set.powers());
        }
        if (collected == null || collected.isEmpty()) return NONE;
        collected.removeIf(id -> ApoliPowers.get(id) == null);
        return collected.isEmpty() ? NONE : List.copyOf(collected);
    }

    public static void applyTo(Entity entity) {
        if (!ANY || entity.level().isClientSide()) return;
        List<ResourceLocation> desired = powersFor(entity.getType());
        PowerContainer container = desired.isEmpty()
            ? PowerContainer.of(entity)
            : PowerContainerAttachment.getOrCreate(entity);
        if (container == null) return;
        if (desired.isEmpty() && !container.allSources().contains(SOURCE)) return;
        reconcile(container, desired);
    }

    private static void reconcile(PowerContainer container, List<ResourceLocation> desired) {
        List<ResourceLocation> stale = null;
        for (ResourceLocation power : container.allPowers()) {
            if (desired.contains(power)) continue;
            if (!container.sourcesOf(power).contains(SOURCE)) continue;
            if (stale == null) stale = new ArrayList<>(2);
            stale.add(power);
        }
        if (stale != null) {
            for (int i = 0; i < stale.size(); i++) container.removePower(stale.get(i), SOURCE);
        }
        for (int i = 0; i < desired.size(); i++) container.addPower(desired.get(i), SOURCE);
    }

    public static void reapplyAll(MinecraftServer server) {
        BY_TYPE.clear();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) applyTo(entity);
        }
    }
}
