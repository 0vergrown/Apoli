package dev.overgrown.apoli.packassets;

import dev.overgrown.apoli.Apoli;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Data pack textures: PNG files under data/<namespace>/textures/ are sent to every player and registered on their
// game as <namespace>:textures/..., so powers can use them without a resource pack.
public final class PackTextures {
    private PackTextures() {}

    public static final int MAX_FILE = 2 * 1024 * 1024;
    public static final int MAX_TOTAL = 32 * 1024 * 1024;
    public static final int MAX_COUNT = 1024;
    public static final int MAX_ID_LENGTH = 200;

    private record Stored(TextureNetwork.Listed listed, byte[] data) {}

    private static volatile Map<ResourceLocation, Stored> stored = Map.of();
    private static volatile int generation;
    private static volatile long storedBytes;
    private static final Outbox<ResourceLocation> OUTBOX = new Outbox<>("data pack textures", TextureNetwork.Chunk::new);

    public static void init() {
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) ->
            event.addListener((ResourceManagerReloadListener) manager -> load(manager)));
        // Fires when a player joins and after /reload. An empty list tells the client to drop what it had.
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> event.getRelevantPlayers().forEach(PackTextures::sendManifest));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) ->
            OUTBOX.tick(event.getServer().getPlayerList().getPlayers(), generation));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> OUTBOX.forget(event.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> OUTBOX.clear());
    }

    public static void registerPayloads(PayloadRegistrar registrar) {
        registrar.playToClient(TextureNetwork.Manifest.TYPE, TextureNetwork.Manifest.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> dev.overgrown.apoli.packassets.client.ClientPackTextures.onManifest(payload)));
        registrar.playToClient(TextureNetwork.Chunk.TYPE, TextureNetwork.Chunk.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> dev.overgrown.apoli.packassets.client.ClientPackTextures.onChunk(payload)));
        registrar.playToServer(TextureNetwork.Request.TYPE, TextureNetwork.Request.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> onRequest((ServerPlayer) ctx.player(), payload)));
    }

    private static void load(ResourceManager manager) {
        Map<ResourceLocation, Stored> found = new TreeMap<>();
        long total = 0;
        Map<ResourceLocation, Resource> resources = new TreeMap<>(
            manager.listResources("textures", id -> id.getPath().endsWith(".png")));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            if (id.toString().length() > MAX_ID_LENGTH) {
                Apoli.LOGGER.warn("[Apoli] Data pack texture {} has a name longer than {} characters and is not sent.", id, MAX_ID_LENGTH);
                continue;
            }
            if (found.size() >= MAX_COUNT) {
                Apoli.LOGGER.warn("[Apoli] More than {} data pack textures; the rest are not sent.", MAX_COUNT);
                break;
            }
            byte[] data;
            try (InputStream in = entry.getValue().open()) {
                data = in.readNBytes(MAX_FILE + 1);
            } catch (IOException e) {
                Apoli.LOGGER.warn("[Apoli] Could not read data pack texture {}: {}", id, e.toString());
                continue;
            }
            if (data.length == 0) continue;
            if (data.length > MAX_FILE) {
                Apoli.LOGGER.warn("[Apoli] Data pack texture {} is larger than {} KB and is not sent.", id, MAX_FILE / 1024);
                continue;
            }
            if (total + data.length > MAX_TOTAL) {
                Apoli.LOGGER.warn("[Apoli] Data pack textures add up to more than {} MB; {} and the rest are not sent.",
                    MAX_TOTAL / (1024 * 1024), id);
                break;
            }
            total += data.length;
            found.put(id, new Stored(new TextureNetwork.Listed(id, sha1(data), data.length), data));
        }
        stored = found;
        storedBytes = total;
        generation++;
        if (!found.isEmpty()) Apoli.LOGGER.info("[Apoli] Loaded {} data pack texture(s), {} KB.", found.size(), total / 1024);
    }

    public static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean hex40(String s) {
        if (s.length() != 40) return false;
        for (int i = 0; i < 40; i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static void sendManifest(ServerPlayer player) {
        if (player.connection == null || !player.connection.hasChannel(TextureNetwork.Manifest.TYPE)) return;
        List<TextureNetwork.Listed> list = new ArrayList<>(stored.size());
        for (Stored s : stored.values()) list.add(s.listed());
        OUTBOX.reset(player.getUUID());
        PacketDistributor.sendToPlayer(player, new TextureNetwork.Manifest(generation, list));
    }

    private static void onRequest(ServerPlayer player, TextureNetwork.Request request) {
        if (request.generation() != generation) return;
        Map<ResourceLocation, byte[]> files = new HashMap<>();
        Map<ResourceLocation, Stored> current = stored;
        for (ResourceLocation id : request.ids()) {
            Stored s = current.get(id);
            if (s != null) files.put(id, s.data());
        }
        OUTBOX.request(player, generation, request.first(), request.ids(), files, 2 * storedBytes + Outbox.CHUNK, MAX_COUNT);
    }
}
