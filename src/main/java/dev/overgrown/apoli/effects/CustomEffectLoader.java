package dev.overgrown.apoli.effects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.loader.IdWildcards;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CustomEffectLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LoggerFactory.getLogger("CustomEffectLoader");
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    public CustomEffectLoader() {
        super(GSON, "effects");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<ResourceLocation, CustomEffect> byId = new HashMap<>(object.size());
        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation id = entry.getKey();
            CustomEffect.codec(id).parse(IdWildcards.apply(new Dynamic<>(JsonOps.INSTANCE, entry.getValue()), id))
                    .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse custom power {}: {}", id, err))
                    .ifPresent(set -> {
                        if (BuiltInRegistries.MOB_EFFECT.containsKey(id) && !CustomEffectRegistry.byId.containsKey(id)) {
                            LOG.error("[Apoli] Failed to parse custom power {}: Identifier was already present in Registry!", id);
                            return;
                        }

                        CustomEffect existing = byId.get(id);
                        if (existing == null || set.loadingPriority() >= existing.loadingPriority()) {
                            byId.put(id, set);
                        }
                    });
        }
        List<CustomEffect> loaded = new ArrayList<>(byId.values());
        CustomEffectRegistry.commit(loaded);
        if (!loaded.isEmpty()) {
            LOG.info("[Apoli] Loaded {} custom effect(s).", loaded.size());
        }
    }
}
