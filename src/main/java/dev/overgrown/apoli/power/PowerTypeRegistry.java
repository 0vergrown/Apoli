package dev.overgrown.apoli.power;

import dev.overgrown.apoli.alias.AliasRegistry;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PowerTypeRegistry {
    private static final Map<ResourceLocation, PowerType<?>> BY_ID = new HashMap<>();
    private static final AliasRegistry ALIASES = new AliasRegistry();

    private PowerTypeRegistry() {}

    /** Per-registry alias map for PowerType ids. */
    public static AliasRegistry aliases() {
        return ALIASES;
    }

    public static ResourceLocation resolveId(ResourceLocation id) {
        return ALIASES.resolve(id);
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
        return BY_ID.get(ALIASES.resolve(id));
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
