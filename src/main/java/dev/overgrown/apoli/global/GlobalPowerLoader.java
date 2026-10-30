package dev.overgrown.apoli.global;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.loader.IdWildcards;
import dev.overgrown.apoli.loader.JsonDataFiles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GlobalPowerLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonDataFiles.Version>> {

    private static final Logger LOG = LogUtils.getLogger();
    private static final String DIR = "global_powers";

    public GlobalPowerLoader() {
        super();
    }

    @Override
    protected @NotNull Map<ResourceLocation, JsonDataFiles.Version> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return JsonDataFiles.loadByPriority(resourceManager, DIR, LOG);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonDataFiles.Version> data, ResourceManager rm, ProfilerFiller profiler) {
        List<GlobalPowerSet> loaded = new ArrayList<>(data.size());
        for (Map.Entry<ResourceLocation, JsonDataFiles.Version> entry : data.entrySet()) {
            ResourceLocation id = entry.getKey();
            GlobalPowerSet.codec(id).parse(IdWildcards.apply(new Dynamic<>(dev.overgrown.apoli.codec.ApoliOps.of(JsonOps.INSTANCE), entry.getValue().json()), id))
                .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse global power set {}: {}", id, err))
                .ifPresent(loaded::add);
        }
        GlobalPowers.replaceAll(loaded);
        if (!loaded.isEmpty()) {
            LOG.info("[Apoli] Loaded {} global power set(s).", loaded.size());
        }
    }
}
