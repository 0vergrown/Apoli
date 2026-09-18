package dev.overgrown.apoli.power;

import dev.overgrown.apoli.power.builtin.MultiplePower;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ApoliPowers {
    private static volatile Map<ResourceLocation, Power> POWERS = Map.of();
    private static volatile Set<ResourceLocation> SUB_POWERS = Set.of();
    private static volatile Set<ResourceLocation> TYPES_IN_USE = Set.of();
    private static volatile Map<String, List<ResourceLocation>> BY_TAG = Map.of();

    private static volatile int GENERATION = 0;

    private ApoliPowers() {}

    public static int generation() {
        return GENERATION;
    }

    public static @Nullable Power get(ResourceLocation id) {
        return POWERS.get(id);
    }

    public static Map<ResourceLocation, Power> view() {
        return Collections.unmodifiableMap(POWERS);
    }

    public static boolean isSubPower(ResourceLocation id) {
        return SUB_POWERS.contains(id);
    }

    public static Set<ResourceLocation> grantableIds() {
        Set<ResourceLocation> out = new HashSet<>(POWERS.keySet());
        out.removeAll(SUB_POWERS);
        return out;
    }

    public static List<ResourceLocation> withTag(String tag) {
        List<ResourceLocation> hit = BY_TAG.get(tag);
        return hit == null ? List.of() : hit;
    }

    public static boolean anyTagged() {
        return !BY_TAG.isEmpty();
    }

    public static boolean hasAnyTag(ResourceLocation id, List<String> tags) {
        Power power = POWERS.get(id);
        if (power == null || power.tags().isEmpty()) return false;
        for (int i = 0; i < tags.size(); i++) {
            if (power.hasTag(tags.get(i))) return true;
        }
        return false;
    }

    public static boolean anyOfType(ResourceLocation canonicalTypeId) {
        return TYPES_IN_USE.contains(canonicalTypeId);
    }

    public static void replaceAll(Map<ResourceLocation, Power> loaded) {
        POWERS = Map.copyOf(loaded);
        Set<ResourceLocation> subs = new HashSet<>();
        Set<ResourceLocation> types = new HashSet<>();
        Map<String, List<ResourceLocation>> byTag = new HashMap<>();
        for (Map.Entry<ResourceLocation, Power> entry : POWERS.entrySet()) {
            Power p = entry.getValue();
            if (p.config() instanceof MultiplePower.Cfg cfg) subs.addAll(cfg.subPowerIds());
            types.add(PowerTypeRegistry.resolveId(p.typeId()));
            for (String tag : p.tags()) {
                byTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(entry.getKey());
            }
        }
        byTag.replaceAll((tag, ids) -> List.copyOf(ids));
        SUB_POWERS = Set.copyOf(subs);
        TYPES_IN_USE = Set.copyOf(types);
        BY_TAG = Map.copyOf(byTag);
        GENERATION++;
    }
}
