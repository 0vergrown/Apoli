package dev.overgrown.apoli.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses {@code data/<ns>/powers/<id>.json} into {@link ApoliPowers}. Sync to
 * clients is handled by NeoForge's {@code OnDatapackSyncEvent} in
 * {@code Apoli.onDatapackSync}, so this listener is intentionally
 * side-effect-free beyond populating the registry — that's important because
 * NeoForge's reload pipeline also runs server-data listeners during
 * {@code CreateWorldScreen.openFresh} (the world-selection screen's datapack
 * inspection), where no server is actually running.
 */
public final class ApoliReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIR = "powers";

    public ApoliReloadListener() {
        super(GSON, DIR);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, Power> loaded = new HashMap<>(data.size());
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            ResourceLocation id = e.getKey();
            JsonElement json = e.getValue();
            Power.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> LOG.error("Failed to parse power {}: {}", id, err))
                .ifPresent(power -> loaded.put(id, power));
        }
        ApoliPowers.replaceAll(loaded);
        LOG.info("[Apoli] Loaded {} power(s).", loaded.size());
    }
}
