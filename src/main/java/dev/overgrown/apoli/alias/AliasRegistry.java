package dev.overgrown.apoli.alias;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AliasRegistry {
    private final Map<ResourceLocation, ResourceLocation> typeAliases = new HashMap<>();

    public AliasRegistry() {}

    public void registerTypeAlias(ResourceLocation oldId, ResourceLocation canonical) {
        ResourceLocation existing = typeAliases.putIfAbsent(oldId, canonical);
        if (existing != null && !existing.equals(canonical)) {
            throw new IllegalStateException(
                "Conflicting type alias: " + oldId + " -> " + existing + " vs " + canonical);
        }
    }

    public ResourceLocation resolve(ResourceLocation id) {
        return typeAliases.getOrDefault(id, id);
    }

    public Map<ResourceLocation, ResourceLocation> view() {
        return Collections.unmodifiableMap(typeAliases);
    }

    public int size() {
        return typeAliases.size();
    }
}
