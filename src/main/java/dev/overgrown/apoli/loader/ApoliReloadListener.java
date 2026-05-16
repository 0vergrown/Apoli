package dev.overgrown.apoli.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public final class ApoliReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIR = "powers";

    private MinecraftServer server;

    public ApoliReloadListener() {
        super(GSON, DIR);
    }

    public void attachServer(MinecraftServer server) {
        this.server = server;
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

        if (server != null) {
            ApoliNetwork.broadcastPowers(server, SyncPowersS2C.fromCurrent());
        }
    }
}