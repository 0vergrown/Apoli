package dev.overgrown.apoli.global;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.loader.IdWildcards;
import dev.overgrown.apoli.loader.JsonFormatParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GlobalPowerLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIR = "global_powers";

    public GlobalPowerLoader() {
        super();
    }

    @Override
    protected @NotNull Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return JsonFormatParser.apply(resourceManager, DIR, GSON, LOG);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, GlobalPowerSet> byId = new HashMap<>(data.size());
        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            ResourceLocation id = entry.getKey();
            GlobalPowerSet.codec(id).parse(IdWildcards.apply(new Dynamic<>(dev.overgrown.apoli.codec.ApoliOps.of(JsonOps.INSTANCE), entry.getValue()), id))
                .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse global power set {}: {}", id, err))
                .ifPresent(set -> {
                    GlobalPowerSet existing = byId.get(id);
                    if (existing == null || set.loadingPriority() >= existing.loadingPriority()) {
                        byId.put(id, set);
                    }
                });
        }
        List<GlobalPowerSet> loaded = new ArrayList<>(byId.values());
        GlobalPowers.replaceAll(loaded);
        if (!loaded.isEmpty()) {
            LOG.info("[Apoli] Loaded {} global power set(s).", loaded.size());
        }
    }
}
