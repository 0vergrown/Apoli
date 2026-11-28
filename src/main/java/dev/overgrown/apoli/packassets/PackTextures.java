package dev.overgrown.apoli.packassets;

import dev.overgrown.apoli.Apoli;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
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
import java.util.UUID;

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
        PayloadTypeRegistry.playS2C().register(TextureNetwork.Manifest.TYPE, TextureNetwork.Manifest.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TextureNetwork.Chunk.TYPE, TextureNetwork.Chunk.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TextureNetwork.Request.TYPE, TextureNetwork.Request.STREAM_CODEC);
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return Apoli.id("data_pack_textures");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                load(manager);
            }
        });
        // Fires when a player joins and after /reload. An empty list tells the client to drop what it had.
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> sendManifest(player));
        ServerTickEvents.END_SERVER_TICK.register(server -> OUTBOX.tick(server.getPlayerList().getPlayers(), generation));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUUID();
            server.execute(() -> OUTBOX.forget(id));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> OUTBOX.clear());
        ServerPlayNetworking.registerGlobalReceiver(TextureNetwork.Request.TYPE,
            (request, context) -> onRequest(context.player(), request));
    }

    private static void load(ResourceManager manager) {
        Map<ResourceLocation, Stored> found = new TreeMap<>();
        long total = 0;
        Map<ResourceLocation, Resource> resources = PackAssetsConfig.get().dataPackTextures()
            ? new TreeMap<>(manager.listResources("textures", id -> id.getPath().endsWith(".png")))
            : Map.of();
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
        if (player.connection == null || !ServerPlayNetworking.canSend(player, TextureNetwork.Manifest.TYPE)) return;
        List<TextureNetwork.Listed> list = new ArrayList<>(stored.size());
        for (Stored s : stored.values()) list.add(s.listed());
        OUTBOX.reset(player.getUUID());
        ServerPlayNetworking.send(player, new TextureNetwork.Manifest(generation, list));
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
