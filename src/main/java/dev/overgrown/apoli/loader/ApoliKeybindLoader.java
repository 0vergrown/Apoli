package dev.overgrown.apoli.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.keybind.ApoliKeybinds;
import dev.overgrown.apoli.keybind.Keybind;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApoliKeybindLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIR = "keybinds";

    private MinecraftServer server;

    public ApoliKeybindLoader() {
        super(GSON, DIR);
    }

    public void attachServer(MinecraftServer server) {
        this.server = server;
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

        if (server != null) {
            ApoliNetwork.broadcastKeybinds(server, SyncKeybindsS2C.fromCurrent());
        }
    }
}
