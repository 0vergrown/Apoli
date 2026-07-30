package dev.overgrown.apoli.network;

import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.network.payload.SyncPowersChunkS2C;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PowerSyncCache {

    private static final int SLICE_BYTES = 512 * 1024;

    private static int builtGeneration = -1;
    private static List<SyncPowersChunkS2C> chunks = List.of();

    private PowerSyncCache() {}

    public static void broadcast(MinecraftServer server) {
        ensureCurrent();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            sendTo(p);
        }
    }

    public static void sendTo(ServerPlayer player) {
        ensureCurrent();
        for (SyncPowersChunkS2C c : chunks) {
            PacketDistributor.sendToPlayer(player, c);
        }
    }

    private static void ensureCurrent() {
        int gen = ApoliPowers.generation();
        if (gen == builtGeneration) {
            return;
        }
        Map<ResourceLocation, String> raw = new HashMap<>();
        for (Map.Entry<ResourceLocation, Power> e : ApoliPowers.view().entrySet()) {
            var encoded = Power.CODEC.encodeStart(JsonOps.INSTANCE, e.getValue());
            var json = encoded.result();
            if (json.isEmpty()) {
                Apoli.LOGGER.error("[Apoli] Power {} could not be re-encoded for client sync and will be missing "
                    + "on every client (it still works on the server): {}",
                    e.getKey(), encoded.error().map(err -> err.message()).orElse("unknown error"));
                continue;
            }
            raw.put(e.getKey(), json.get().toString());
        }

        byte[] gz = SyncPowersChunkS2C.encodeBlob(raw);
        int total = (gz.length + SLICE_BYTES - 1) / SLICE_BYTES;
        List<SyncPowersChunkS2C> built = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            int from = i * SLICE_BYTES;
            built.add(new SyncPowersChunkS2C(i, total, Arrays.copyOfRange(gz, from, Math.min(gz.length, from + SLICE_BYTES))));
        }
        chunks = List.copyOf(built);
        builtGeneration = gen;
    }
}
