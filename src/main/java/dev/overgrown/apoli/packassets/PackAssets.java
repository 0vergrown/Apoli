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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;

// Data pack assets: the assets/ folder of every data pack in the world's datapacks folder is sent to players and
// loaded on their game as a resource pack. This reloads the player's resources, so unlike data pack textures it also
// covers models, sounds, fonts, language files and animated textures.
public final class PackAssets {
    private PackAssets() {}

    public static final int MAX_FILE = 16 * 1024 * 1024;
    public static final int MAX_TOTAL = 64 * 1024 * 1024;
    public static final int MAX_COUNT = 4096;

    private record Stored(AssetNetwork.Listed listed, byte[] data) {}

    private static volatile Map<String, Stored> stored = Map.of();
    private static volatile String packHash = "";
    private static volatile int generation;
    // Set when data packs reload; the files are read again the next time a list is sent.
    private static volatile boolean stale = true;
    private static volatile long storedBytes;
    private static final Outbox<String> OUTBOX = new Outbox<>("data pack assets", AssetNetwork.Chunk::new);

    public static void init() {
        PayloadTypeRegistry.playS2C().register(AssetNetwork.Manifest.TYPE, AssetNetwork.Manifest.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(AssetNetwork.Chunk.TYPE, AssetNetwork.Chunk.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AssetNetwork.Request.TYPE, AssetNetwork.Request.STREAM_CODEC);
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return Apoli.id("data_pack_assets");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                stale = true;
            }
        });
        // Fires when a player joins and after /reload. An empty list tells the client to drop what it had.
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> sendManifest(player));
        ServerTickEvents.END_SERVER_TICK.register(server -> OUTBOX.tick(server.getPlayerList().getPlayers(), generation));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUUID();
            server.execute(() -> OUTBOX.forget(id));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            OUTBOX.clear();
            stored = Map.of();
            stale = true;
        });
        ServerPlayNetworking.registerGlobalReceiver(AssetNetwork.Request.TYPE,
            (request, context) -> onRequest(context.player(), request));
    }

    private static void load(MinecraftServer server) {
        Map<String, Stored> found = new TreeMap<>();
        Path packs = server.getWorldPath(LevelResource.DATAPACK_DIR);
        // Later packs win, as they do for data.
        for (Pack pack : PackAssetsConfig.get().dataPackAssets()
            ? server.getPackRepository().getSelectedPacks() : List.<Pack>of()) {
            String name = pack.getId();
            if (!name.startsWith("file/")) continue;
            try {
                Path root = packs.resolve(name.substring("file/".length()));
                if (Files.isDirectory(root)) {
                    collect(root.resolve("assets"), found);
                } else if (Files.isRegularFile(root)) {
                    try (FileSystem zip = FileSystems.newFileSystem(root)) {
                        collect(zip.getPath("assets"), found);
                    }
                }
            } catch (IOException | RuntimeException e) {
                Apoli.LOGGER.warn("[Apoli] Could not read the assets of data pack {}: {}", name, e.toString());
            }
        }
        long total = 0;
        StringBuilder all = new StringBuilder();
        for (Stored s : found.values()) {
            total += s.data().length;
            all.append(s.listed().path()).append('\0').append(s.listed().hash()).append('\n');
        }
        stored = found;
        storedBytes = total;
        packHash = found.isEmpty() ? "" : PackTextures.sha1(all.toString().getBytes(StandardCharsets.UTF_8));
        generation++;
        if (!found.isEmpty()) Apoli.LOGGER.info("[Apoli] Loaded {} data pack asset file(s), {} KB.", found.size(), total / 1024);
    }

    private static void collect(Path assets, Map<String, Stored> out) throws IOException {
        if (!Files.isDirectory(assets)) return;
        List<Path> files;
        try (Stream<Path> walk = Files.walk(assets)) {
            files = walk.filter(Files::isRegularFile).sorted().toList();
        }
        long total = 0;
        for (Stored s : out.values()) total += s.data().length;
        for (Path file : files) {
            String path = assets.relativize(file).toString().replace('\\', '/');
            if (!valid(path)) {
                Apoli.LOGGER.warn("[Apoli] Data pack asset {} has a name the game cannot use and is not sent.", path);
                continue;
            }
            long size = Files.size(file);
            if (size == 0) continue;
            if (size > MAX_FILE) {
                Apoli.LOGGER.warn("[Apoli] Data pack asset {} is larger than {} MB and is not sent.", path, MAX_FILE / (1024 * 1024));
                continue;
            }
            Stored previous = out.get(path);
            long without = total - (previous == null ? 0 : previous.data().length);
            if (previous == null && out.size() >= MAX_COUNT) {
                Apoli.LOGGER.warn("[Apoli] More than {} data pack asset files; {} and the rest are not sent.", MAX_COUNT, path);
                return;
            }
            if (without + size > MAX_TOTAL) {
                Apoli.LOGGER.warn("[Apoli] Data pack assets add up to more than {} MB; {} and the rest are not sent.",
                    MAX_TOTAL / (1024 * 1024), path);
                return;
            }
            byte[] data = Files.readAllBytes(file);
            total = without + data.length;
            out.put(path, new Stored(new AssetNetwork.Listed(path, PackTextures.sha1(data), data.length), data));
        }
    }

    // path is <namespace>/<path inside the namespace>, both made of characters an ID allows.
    public static boolean valid(String path) {
        int slash = path.indexOf('/');
        return slash > 0 && slash < path.length() - 1 && path.length() <= AssetNetwork.MAX_PATH
            && ResourceLocation.tryBuild(path.substring(0, slash), path.substring(slash + 1)) != null;
    }

    private static void sendManifest(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || player.connection == null || !ServerPlayNetworking.canSend(player, AssetNetwork.Manifest.TYPE)) return;
        if (stale) {
            stale = false;
            load(server);
        }
        List<AssetNetwork.Listed> list = new ArrayList<>(stored.size());
        for (Stored s : stored.values()) list.add(s.listed());
        OUTBOX.reset(player.getUUID());
        int from = 0;
        do {
            int to = Math.min(list.size(), from + AssetNetwork.MANIFEST_BATCH);
            ServerPlayNetworking.send(player, new AssetNetwork.Manifest(generation, packHash, from == 0, to == list.size(),
                List.copyOf(list.subList(from, to))));
            from = to;
        } while (from < list.size());
    }

    private static void onRequest(ServerPlayer player, AssetNetwork.Request request) {
        if (request.generation() != generation) return;
        Map<String, byte[]> files = new HashMap<>();
        Map<String, Stored> current = stored;
        for (String path : request.paths()) {
            Stored s = current.get(path);
            if (s != null) files.put(path, s.data());
        }
        OUTBOX.request(player, generation, request.first(), request.paths(), files, 2 * storedBytes + Outbox.CHUNK, MAX_COUNT);
    }
}
