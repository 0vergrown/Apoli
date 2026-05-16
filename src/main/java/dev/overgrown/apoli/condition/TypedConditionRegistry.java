package dev.overgrown.apoli.condition;

import dev.overgrown.apoli.alias.AliasRegistry;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.alias.AliasingOptions;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class TypedConditionRegistry<CTX> {
    private final String groupName;
    private final Map<ResourceLocation, Entry<CTX, ?>> byId = new HashMap<>();
    private final AliasRegistry aliases = new AliasRegistry();

    public TypedConditionRegistry(String groupName) {
        this.groupName = groupName;
    }

    public String groupName() {
        return groupName;
    }

    /** Per-registry alias map. Aliases declared here don't leak to other registries. */
    public AliasRegistry aliases() {
        return aliases;
    }

    public ResourceLocation resolveId(ResourceLocation id) {
        return aliases.resolve(id);
    }

    public <C> ConditionType<CTX, C> register(ResourceLocation id, ConditionType<CTX, C> type) {
        return register(id, type, AliasingOptions.NONE);
    }

    public <C> ConditionType<CTX, C> register(ResourceLocation id, ConditionType<CTX, C> type, AliasingOptions opts) {
        if (byId.containsKey(id)) {
            throw new IllegalStateException("Duplicate " + groupName + " condition: " + id);
        }
        ConditionType<CTX, C> wrapped = opts.fieldAliases().isEmpty()
            ? type
            : new WrappedFieldAlias<>(type, opts.fieldAliases());
        byId.put(id, new Entry<>(wrapped, opts));
        for (ResourceLocation old : opts.typeAliases()) {
            aliases.registerTypeAlias(old, id);
        }
        return wrapped;
    }

    public @Nullable ConditionType<CTX, ?> get(ResourceLocation id) {
        Entry<CTX, ?> e = byId.get(aliases.resolve(id));
        return e == null ? null : e.type;
    }

    public Map<ResourceLocation, Entry<CTX, ?>> view() {
        return Map.copyOf(byId);
    }

    public record Entry<CTX, C>(ConditionType<CTX, C> type, AliasingOptions aliases) {}

    private record WrappedFieldAlias<CTX, C>(
        ConditionType<CTX, C> delegate, Map<String, String> oldToNew
    ) implements ConditionType<CTX, C> {
        @Override
        public com.mojang.serialization.MapCodec<C> codec() {
            return AliasingMapCodec.wrap(delegate.codec(), oldToNew);
        }

        @Override
        public boolean test(C config, CTX ctx) {
            return delegate.test(config, ctx);
        }
    }
}
