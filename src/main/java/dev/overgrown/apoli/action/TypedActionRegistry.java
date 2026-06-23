package dev.overgrown.apoli.action;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.alias.AliasRegistry;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.alias.DefaultInjectingMapCodec;
import dev.overgrown.apoli.alias.NamespaceAlias;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class TypedActionRegistry<CTX> {
    private final String groupName;
    private final Map<ResourceLocation, Entry<CTX, ?>> byId = new HashMap<>();
    private final AliasRegistry aliases = new AliasRegistry();
    private final Map<ResourceLocation, Map<String, String>> aliasDefaults = new HashMap<>();

    public TypedActionRegistry(String groupName) {
        this.groupName = groupName;
    }

    public String groupName() {
        return groupName;
    }

    public AliasRegistry aliases() {
        return aliases;
    }

    public ResourceLocation resolveId(ResourceLocation id) {
        ResourceLocation typeResolved = aliases.resolve(id);
        if (byId.containsKey(typeResolved)) return typeResolved;
        if (NamespaceAlias.hasAlias(id.getNamespace())) {
            return aliases.resolve(NamespaceAlias.resolve(id));
        }
        return typeResolved;
    }

    public <C> ActionType<CTX, C> register(ResourceLocation id, ActionType<CTX, C> type) {
        return register(id, type, AliasingOptions.NONE);
    }

    public <C> ActionType<CTX, C> register(ResourceLocation id, ActionType<CTX, C> type, AliasingOptions opts) {
        if (byId.containsKey(id)) {
            throw new IllegalStateException("Duplicate " + groupName + " action: " + id);
        }
        ActionType<CTX, C> wrapped = opts.fieldAliases().isEmpty()
            ? type
            : new WrappedFieldAlias<>(type, opts.fieldAliases());
        byId.put(id, new Entry<>(wrapped, opts));
        for (ResourceLocation old : opts.typeAliases()) {
            aliases.registerTypeAlias(old, id);
        }
        aliasDefaults.putAll(opts.typeAliasDefaults());
        return wrapped;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public MapCodec<?> applyAliasDefaults(ResourceLocation originalId, MapCodec<?> codec) {
        Map<String, String> defaults = aliasDefaults.get(originalId);
        if (defaults == null || defaults.isEmpty()) return codec;
        return DefaultInjectingMapCodec.wrap((MapCodec) codec, defaults);
    }

    public @Nullable ActionType<CTX, ?> get(ResourceLocation id) {
        Entry<CTX, ?> e = byId.get(aliases.resolve(id));
        if (e != null) return e.type;
        if (NamespaceAlias.hasAlias(id.getNamespace())) {
            e = byId.get(aliases.resolve(NamespaceAlias.resolve(id)));
            if (e != null) return e.type;
        }
        return null;
    }

    public Map<ResourceLocation, Entry<CTX, ?>> view() {
        return Map.copyOf(byId);
    }

    public record Entry<CTX, C>(ActionType<CTX, C> type, AliasingOptions aliases) {}

    private record WrappedFieldAlias<CTX, C>(
        ActionType<CTX, C> delegate, Map<String, String> oldToNew
    ) implements ActionType<CTX, C> {
        @Override
        public com.mojang.serialization.MapCodec<C> codec() {
            return AliasingMapCodec.wrap(delegate.codec(), oldToNew);
        }

        @Override
        public void run(C config, CTX ctx) {
            delegate.run(config, ctx);
        }
    }
}
