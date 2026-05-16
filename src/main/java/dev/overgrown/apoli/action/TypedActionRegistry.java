package dev.overgrown.apoli.action;

import dev.overgrown.apoli.alias.AliasRegistry;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.alias.AliasingOptions;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class TypedActionRegistry<CTX> {
    private final String groupName;
    private final Map<ResourceLocation, Entry<CTX, ?>> byId = new HashMap<>();
    private final AliasRegistry aliases = new AliasRegistry();

    public TypedActionRegistry(String groupName) {
        this.groupName = groupName;
    }

    public String groupName() {
        return groupName;
    }

    /** Per-registry alias map. See note on {@link AliasRegistry}. */
    public AliasRegistry aliases() {
        return aliases;
    }

    public ResourceLocation resolveId(ResourceLocation id) {
        return aliases.resolve(id);
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
        return wrapped;
    }

    public @Nullable ActionType<CTX, ?> get(ResourceLocation id) {
        Entry<CTX, ?> e = byId.get(aliases.resolve(id));
        return e == null ? null : e.type;
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
