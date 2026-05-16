package dev.overgrown.apoli.alias;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AliasingOptions(List<ResourceLocation> typeAliases, Map<String, String> fieldAliases) {
    public static final AliasingOptions NONE = new AliasingOptions(List.of(), Map.of());

    public static AliasingOptions none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<ResourceLocation> typeAliases = new ArrayList<>();
        private final Map<String, String> fieldAliases = new HashMap<>();

        public Builder addTypeAlias(ResourceLocation oldId) {
            typeAliases.add(oldId); return this;
        }

        public Builder addTypeAlias(String oldId) {
            return addTypeAlias(ResourceLocation.tryParse(oldId));
        }

        public Builder renameField(String oldName, String newName) {
            fieldAliases.put(oldName, newName);
            return this;
        }

        public AliasingOptions build() {
            return new AliasingOptions(List.copyOf(typeAliases), Map.copyOf(fieldAliases));
        }
    }
}