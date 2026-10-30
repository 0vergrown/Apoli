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

    public ApoliKeybindLoader() {
        super();
    }

    @Override
    protected @NotNull Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return JsonDataFiles.load(resourceManager, DIR, LOG);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, Keybind> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : data.entrySet()) {
            ResourceLocation id = e.getKey();
            Keybind.CODEC_NO_ID.parse(dev.overgrown.apoli.codec.ApoliOps.of(JsonOps.INSTANCE), e.getValue())
                .resultOrPartial(err -> LOG.error("Failed to parse keybind {}: {}", id, err))
                .ifPresent(kb -> loaded.put(id, kb.withId(id)));
        }
        ApoliKeybinds.replaceAll(loaded);
        LOG.info("[Apoli] Loaded {} data-driven keybind(s).", loaded.size());
    }
}
