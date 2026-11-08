package dev.overgrown.apoli.loader;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.keybind.ApoliKeybinds;
import dev.overgrown.apoli.keybind.Keybind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApoliKeybindLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String DIR = "keybinds";
    private volatile Map<ResourceLocation, JsonDataFiles.Source> sources = Map.of();

    public ApoliKeybindLoader() {
        super();
    }

    @Override
    protected @NotNull Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<ResourceLocation, JsonDataFiles.Source> found = new java.util.HashMap<>();
        Map<ResourceLocation, JsonElement> loaded = JsonDataFiles.load(resourceManager, DIR, LOG, found);
        sources = found;
        return loaded;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, Keybind> loaded = new LinkedHashMap<>();
        Map<ResourceLocation, JsonDataFiles.Source> files = sources;
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            ResourceLocation id = e.getKey();
            LoadProblems.parse("keybind", id, files.get(id), "$", Keybind.CODEC_NO_ID,
                    new com.mojang.serialization.Dynamic<>(dev.overgrown.apoli.codec.ApoliOps.of(JsonOps.INSTANCE), e.getValue()))
                .ifPresent(kb -> loaded.put(id, kb.withId(id)));
        }
        ApoliKeybinds.replaceAll(loaded);
        LOG.info("[Apoli] Loaded {} data-driven keybind(s).", loaded.size());
    }
}
