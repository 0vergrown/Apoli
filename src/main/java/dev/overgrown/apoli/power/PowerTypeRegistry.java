package dev.overgrown.apoli.power;

import dev.overgrown.apoli.alias.AliasDefault;
import dev.overgrown.apoli.alias.AliasRegistry;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.alias.NamespaceAlias;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class PowerTypeRegistry {
    private static final Map<ResourceLocation, PowerType<?>> BY_ID = new HashMap<>();
    private static final AliasRegistry ALIASES = new AliasRegistry();
    private static final Map<ResourceLocation, List<AliasDefault<?>>> ALIAS_DEFAULTS = new HashMap<>();
    private static final Map<ResourceLocation, Map<String, String>> ALIAS_FIELD_RENAMES = new HashMap<>();

    private PowerTypeRegistry() {}

    public static void registerAliasDefaults(ResourceLocation aliasId, AliasDefault<?>... defaults) {
        ALIAS_DEFAULTS.computeIfAbsent(aliasId, k -> new ArrayList<>(defaults.length))
            .addAll(List.of(defaults));
    }

    public static List<AliasDefault<?>> aliasDefaults(ResourceLocation aliasId) {
        return ALIAS_DEFAULTS.getOrDefault(aliasId, List.of());
    }

    public static void registerAliasFieldRenames(ResourceLocation aliasId, Map<String, String> oldToNew) {
        ALIAS_FIELD_RENAMES.merge(aliasId, Map.copyOf(oldToNew), (a, b) -> {
            Map<String, String> merged = new HashMap<>(a);
            merged.putAll(b);
            return Map.copyOf(merged);
        });
    }

    public static Map<String, String> aliasFieldRenames(ResourceLocation aliasId) {
        return ALIAS_FIELD_RENAMES.getOrDefault(aliasId, Map.of());
    }

    public static AliasRegistry aliases() {
        return ALIASES;
    }

    public static ResourceLocation resolveId(ResourceLocation id) {
        ResourceLocation typeResolved = ALIASES.resolve(id);
        if (BY_ID.containsKey(typeResolved)) return typeResolved;
        if (NamespaceAlias.hasAlias(id.getNamespace())) {
            return ALIASES.resolve(NamespaceAlias.resolve(id));
        }
        return typeResolved;
    }

    public static <T extends PowerType<?>> T register(ResourceLocation id, T type) {
        return register(id, type, AliasingOptions.NONE);
    }

    public static <T extends PowerType<?>> T register(ResourceLocation id, T type, AliasingOptions opts) {
        if (BY_ID.containsKey(id)) {
            throw new IllegalStateException("Duplicate PowerType: " + id);
        }
        PowerType<?> wrapped = opts.fieldAliases().isEmpty()
            ? type
            : wrapWithFieldAliasesRaw(type, opts.fieldAliases());
        BY_ID.put(id, wrapped);
        for (ResourceLocation old : opts.typeAliases()) {
            ALIASES.registerTypeAlias(old, id);
        }
        return type;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static PowerType<?> wrapWithFieldAliasesRaw(PowerType<?> inner, Map<String, String> oldToNew) {
        return wrapWithFieldAliases((PowerType) inner, oldToNew);
    }

    public static @Nullable PowerType<?> get(ResourceLocation id) {
        PowerType<?> direct = BY_ID.get(ALIASES.resolve(id));
        if (direct != null) return direct;
        if (NamespaceAlias.hasAlias(id.getNamespace())) {
            return BY_ID.get(ALIASES.resolve(NamespaceAlias.resolve(id)));
        }
        return null;
    }

    public static Map<ResourceLocation, PowerType<?>> view() {
        return Collections.unmodifiableMap(BY_ID);
    }

    private static <C> PowerType<C> wrapWithFieldAliases(PowerType<C> inner, Map<String, String> oldToNew) {
        return new PowerType<>() {
            @Override
            public com.mojang.serialization.MapCodec<C> configCodec() {
                return AliasingMapCodec.wrap(inner.configCodec(), oldToNew);
            }

            @Override
            public void onAdded(ResourceLocation id, C cfg, PowerContainer h, ResourceLocation s) {
                inner.onAdded(id, cfg, h, s);
            }

            @Override
            public void onRemoved(ResourceLocation id, C cfg, PowerContainer h, ResourceLocation s) {
                inner.onRemoved(id, cfg, h, s);
            }

            @Override
            public void tick(ResourceLocation id, C cfg, PowerContainer h) {
                inner.tick(id, cfg, h);
            }

            @Override
            public boolean isActive(ResourceLocation id, C cfg, EntityCtx ctx) {
                return inner.isActive(id, cfg, ctx);
            }
        };
    }
}
