package dev.overgrown.apoli.network;

import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.network.payload.SyncPowersChunkS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PowerSyncCache {

    private static final int SLICE_BYTES = 512 * 1024;

    private static final int LEGACY_MAX_CHARS = 32767;

    private static final Set<UUID> DEFERRED = ConcurrentHashMap.newKeySet();

    private static int builtGeneration = -1;
    private static SyncPowersS2C legacyPayload;
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
        if (ServerPlayNetworking.canSend(player, SyncPowersChunkS2C.TYPE)) {
            DEFERRED.remove(player.getUUID());
            for (SyncPowersChunkS2C c : chunks) {
                ServerPlayNetworking.send(player, c);
            }
        } else if (legacyPayload != null) {
            DEFERRED.remove(player.getUUID());
            ServerPlayNetworking.send(player, legacyPayload);
        } else {

            DEFERRED.add(player.getUUID());
        }
    }

    public static void onProtocolEcho(ServerPlayer player) {
        if (!DEFERRED.remove(player.getUUID())) {
            return;
        }
        ensureCurrent();
        if (ServerPlayNetworking.canSend(player, SyncPowersChunkS2C.TYPE)) {
            for (SyncPowersChunkS2C c : chunks) {
                ServerPlayNetworking.send(player, c);
            }
        } else if (legacyPayload != null) {
            ServerPlayNetworking.send(player, legacyPayload);
        } else {

            player.connection.disconnect(Component.literal(
                "This server's Apoli power set is too large for your Apoli build. Update Apoli on your client."));
        }
    }

    public static void forget(UUID player) {
        DEFERRED.remove(player);
    }

    private static void ensureCurrent() {
        int gen = ApoliPowers.generation();
        if (gen == builtGeneration) {
            return;
        }
        Map<ResourceLocation, String> raw = new HashMap<>();
        boolean legacyEncodable = true;
        for (Map.Entry<ResourceLocation, Power> e : ApoliPowers.view().entrySet()) {
            var encoded = Power.CODEC.encodeStart(JsonOps.INSTANCE, e.getValue());
            var json = encoded.result();
            if (json.isEmpty()) {
                Apoli.LOGGER.error("[Apoli] Power {} could not be re-encoded for client sync and will be missing "
                    + "on every client (it still works on the server): {}",
                    e.getKey(), encoded.error().map(err -> err.message()).orElse("unknown error"));
                continue;
            }
            String s = json.get().toString();
            raw.put(e.getKey(), s);
            if (s.length() > LEGACY_MAX_CHARS) {
                legacyEncodable = false;
            }
        }

        byte[] gz = SyncPowersChunkS2C.encodeBlob(raw);
        int total = (gz.length + SLICE_BYTES - 1) / SLICE_BYTES;
        List<SyncPowersChunkS2C> built = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            int from = i * SLICE_BYTES;
            built.add(new SyncPowersChunkS2C(i, total, Arrays.copyOfRange(gz, from, Math.min(gz.length, from + SLICE_BYTES))));
        }
        chunks = List.copyOf(built);
        legacyPayload = legacyEncodable ? new SyncPowersS2C(raw) : null;
        builtGeneration = gen;
        if (!legacyEncodable) {
            Apoli.LOGGER.info("[Apoli] Power sync exceeds the legacy packet format ({} power(s), {} gz slice(s)); "
                + "clients on pre-chunk Apoli builds will be asked to update.", raw.size(), total);
        }
    }
}
