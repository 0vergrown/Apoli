package dev.overgrown.apoli.alias;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class NamespaceAlias {
    private static final Map<String, String> ALIASES = new HashMap<>();

    private NamespaceAlias() {}

    public static void addAlias(String from, String to) {
        String existing = ALIASES.putIfAbsent(from, to);
        if (existing != null && !existing.equals(to)) {
            throw new IllegalStateException(
                "Conflicting namespace alias: " + from + " -> " + existing + " vs " + to);
        }
    }

    public static boolean hasAlias(String namespace) {
        return ALIASES.containsKey(namespace);
    }

    public static ResourceLocation resolve(ResourceLocation id) {
        String target = ALIASES.get(id.getNamespace());
        return target == null ? id : new ResourceLocation(target, id.getPath());
    }

    public static Map<String, String> view() {
        return Collections.unmodifiableMap(ALIASES);
    }
}
