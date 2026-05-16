package dev.overgrown.apoli.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.keybind.ApoliKeybinds;
import dev.overgrown.apoli.keybind.Keybind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses {@code data/<ns>/keybinds/<id>.json} into {@link ApoliKeybinds}. Sync
 * to clients is owned by {@code OnDatapackSyncEvent} in {@code Apoli.java}.
 * See {@link ApoliReloadListener} for the reason this listener avoids
 * touching the network from {@link #apply}.
 */
public final class ApoliKeybindLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIR = "keybinds";

    public ApoliKeybindLoader() {
        super(GSON, DIR);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, Keybind> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            ResourceLocation id = e.getKey();
            Keybind.CODEC_NO_ID.parse(JsonOps.INSTANCE, e.getValue())
                .resultOrPartial(err -> LOG.error("Failed to parse keybind {}: {}", id, err))
                .ifPresent(kb -> loaded.put(id, kb.withId(id)));
        }
        ApoliKeybinds.replaceAll(loaded);
        LOG.info("[Apoli] Loaded {} data-driven keybind(s).", loaded.size());
    }
}
