package dev.overgrown.apoli.alias;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps deprecated type ids onto their canonical replacement. Each typed
 * registry — {@code PowerTypeRegistry}, every {@code TypedConditionRegistry},
 * every {@code TypedActionRegistry} — owns its own instance so that aliases
 * declared for one kind (e.g. condition {@code apoli:and} → {@code apoli:all_of})
 * don't bleed into another kind that legitimately uses the old name as its
 * canonical id (e.g. action {@code apoli:and} stays {@code apoli:and}).
 */
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
