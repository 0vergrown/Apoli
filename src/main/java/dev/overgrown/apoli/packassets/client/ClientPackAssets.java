package dev.overgrown.apoli.packassets.client;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.packassets.AssetNetwork;
import dev.overgrown.apoli.packassets.PackAssets;
import dev.overgrown.apoli.packassets.PackTextures;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Client side of data pack assets. Files are cached under their hash, so each is downloaded once. When every file of
// the server's list is there, they're offered to the game as an always-on resource pack and resources reload.
//
// Reloading is the slow part, so it only happens when the set of files changes. The pack stays loaded after the
// player leaves and is remembered on disk, so the next game start loads it with everything else.
@Environment(EnvType.CLIENT)
public final class ClientPackAssets {
    private ClientPackAssets() {}

    public static final String PACK_NAME = "apoli_data_pack_assets";
    private static final int PACK_FORMAT = 15;

    private static final class Incoming {
        final AssetNetwork.Listed listed;
        byte[] data;
        int received;

        Incoming(AssetNetwork.Listed listed) {
            this.listed = listed;
        }
    }

    private static int generation = -1;
    private static List<AssetNetwork.Listed> arriving = new ArrayList<>();
    private static List<AssetNetwork.Listed> pending = List.of();
    private static String pendingHash = "";
    private static final Map<String, Incoming> INCOMING = new HashMap<>();
    // The files of the pack the game is using now, or null when there is none.
    private static volatile Map<ResourceLocation, Path> active;
    private static String activeHash = "";
    // Whether that pack replaces files of another pack. Such a pack must not outlive the server it came from.
    private static boolean activeOverrides;
    private static boolean restored;
    // A pack that made a resource reload fail. It isn't applied again this game session.
    private static String rejectedHash = "";
    // Ticks since joining while waiting for the server's list; -1 when not waiting.
    private static int waitingForList = -1;
    private static final int LIST_TIMEOUT_TICKS = 200;

    public static void init() {
        restore();
        ClientPlayNetworking.registerGlobalReceiver(AssetNetwork.MANIFEST, (client, handler, buf, sender) -> {
            AssetNetwork.Manifest manifest = AssetNetwork.Manifest.read(buf);
            client.execute(() -> onManifestPart(manifest));
        });
        ClientPlayNetworking.registerGlobalReceiver(AssetNetwork.CHUNK, (client, handler, buf, sender) -> {
            AssetNetwork.Chunk chunk = AssetNetwork.Chunk.read(buf);
            client.execute(() -> onChunk(chunk));
        });
        // The pack is kept when the player leaves; only a download in progress stops.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            generation = -1;
            arriving = new ArrayList<>();
            pending = List.of();
            INCOMING.clear();
            waitingForList = -1;
        }));
        // A server without Apoli sends no list. A pack that replaces other packs' files must not follow the player
        // there, so it's dropped when no list has arrived a few seconds after joining.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> waitingForList = 0));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (waitingForList >= 0 && ++waitingForList > LIST_TIMEOUT_TICKS) {
                waitingForList = -1;
                if (active != null && activeOverrides) {
                    Apoli.LOGGER.info("[Apoli] This server sends no data pack assets; removing the pack from the previous server.");
                    apply(null, "", false, null);
                }
            }
        });
    }

    // Called when a resource reload fails. The pack is always on, so the game can't drop it by itself: drop it here
    // and don't take it again.
    public static void onReloadFailed() {
        if (active == null) return;
        Apoli.LOGGER.error("[Apoli] Resources failed to reload with the server's data pack assets loaded; the pack was removed.");
        rejectedHash = activeHash;
        active = null;
        activeHash = "";
        activeOverrides = false;
        remember(null);
    }

    // The always-on resource pack made of the downloaded files, or null. Called on every pack scan.
    @Nullable
    public static Pack profile() {
        restore();
        Map<ResourceLocation, Path> files = active;
        if (files == null) return null;
        return Pack.create(PACK_NAME, Component.literal("Data pack assets"), true,
            name -> new AssetPackResources(name, files),
            new Pack.Info(Component.literal("Sent by the server (Apoli)"), PACK_FORMAT, FeatureFlagSet.of()),
            PackType.CLIENT_RESOURCES, Pack.Position.TOP, true, PackSource.SERVER);
    }

    private static Path cacheDir() {
        return FabricLoader.getInstance().getGameDir().resolve("apoli-asset-cache");
    }

    private static Path cacheFile(String hash) {
        return cacheDir().resolve(hash);
    }

    // Brings back the pack that was loaded when the game last closed, so it loads with the rest at startup.
    private static synchronized void restore() {
        if (restored) return;
        restored = true;
        try {
            Path state = cacheDir().resolve("active.txt");
            if (!Files.isRegularFile(state)) return;
            List<String> lines = Files.readAllLines(state);
            if (lines.size() < 3 || !PackTextures.hex40(lines.get(0))) return;
            Map<ResourceLocation, Path> files = new HashMap<>();
            for (String line : lines.subList(2, lines.size())) {
                int tab = line.indexOf('\t');
                if (tab < 0) return;
                String path = line.substring(0, tab);
                String hash = line.substring(tab + 1);
                // A missing file means the cache was cleared: start without the pack and let the server send it again.
                if (!PackAssets.valid(path) || !PackTextures.hex40(hash) || !Files.isRegularFile(cacheFile(hash))) return;
                ResourceLocation id = id(path);
                if (id != null) files.put(id, cacheFile(hash));
            }
            active = files;
            activeHash = lines.get(0);
            activeOverrides = lines.get(1).equals("overrides");
        } catch (IOException | RuntimeException e) {
            Apoli.LOGGER.warn("[Apoli] Could not restore data pack assets: {}", e.toString());
        }
    }

    @Nullable
    private static ResourceLocation id(String path) {
        int slash = path.indexOf('/');
        return ResourceLocation.tryBuild(path.substring(0, slash), path.substring(slash + 1));
    }

    private static void remember(@Nullable List<AssetNetwork.Listed> files) {
        try {
            Path state = cacheDir().resolve("active.txt");
            if (files == null) {
                Files.deleteIfExists(state);
                return;
            }
            List<String> lines = new ArrayList<>(files.size() + 2);
            lines.add(activeHash);
            lines.add(activeOverrides ? "overrides" : "adds");
            for (AssetNetwork.Listed f : files) lines.add(f.path() + "\t" + f.hash());
            Files.createDirectories(state.getParent());
            Files.write(state, lines);
        } catch (IOException e) {
            Apoli.LOGGER.warn("[Apoli] Could not remember data pack assets: {}", e.toString());
        }
    }

    private static void onManifestPart(AssetNetwork.Manifest part) {
        if (part.first()) arriving = new ArrayList<>();
        if (arriving.size() + part.files().size() > PackAssets.MAX_COUNT) {
            Apoli.LOGGER.warn("[Apoli] The server's data pack asset list is too long; it was ignored.");
            arriving = new ArrayList<>();
            return;
        }
        arriving.addAll(part.files());
        if (part.last()) {
            List<AssetNetwork.Listed> files = arriving;
            arriving = new ArrayList<>();
            onManifest(part.generation(), part.packHash(), files);
        }
    }

    private static void onManifest(int newGeneration, String packHash, List<AssetNetwork.Listed> files) {
        waitingForList = -1;
        generation = newGeneration;
        INCOMING.clear();
        pending = List.of();
        if (files.isEmpty()) {
            // A pack left over from another server stays unless it replaces other packs' files.
            if (active != null && activeOverrides) apply(null, "", false, null);
            return;
        }
        long total = 0;
        for (AssetNetwork.Listed f : files) {
            total += f.size();
            if (!PackAssets.valid(f.path()) || !PackTextures.hex40(f.hash()) || f.size() <= 0
                || f.size() > PackAssets.MAX_FILE || total > PackAssets.MAX_TOTAL) {
                Apoli.LOGGER.warn("[Apoli] The server's data pack asset list is not usable ({}); it was ignored.", f.path());
                return;
            }
        }
        if (!PackTextures.hex40(packHash) || packHash.equals(activeHash)) return;
        if (packHash.equals(rejectedHash)) {
            Apoli.LOGGER.warn("[Apoli] The server's data pack assets broke a resource reload earlier and are not applied again until the game restarts.");
            return;
        }
        pending = files;
        pendingHash = packHash;
        List<String> missing = new ArrayList<>();
        Set<String> requestedHashes = new HashSet<>();
        for (AssetNetwork.Listed f : pending) {
            Path file = cacheFile(f.hash());
            try {
                if (Files.isRegularFile(file) && Files.size(file) == f.size()) continue;
            } catch (IOException ignored) {
                // Treated as missing.
            }
            // Two paths can share one file; it's only downloaded once.
            if (requestedHashes.add(f.hash())) {
                INCOMING.put(f.path(), new Incoming(f));
                missing.add(f.path());
            }
        }
        if (missing.isEmpty()) {
            finish();
            return;
        }
        Apoli.LOGGER.info("[Apoli] Downloading {} data pack asset file(s) from the server.", missing.size());
        for (int from = 0; from < missing.size(); from += AssetNetwork.REQUEST_BATCH) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            new AssetNetwork.Request(generation, from == 0,
                missing.subList(from, Math.min(missing.size(), from + AssetNetwork.REQUEST_BATCH))).write(buf);
            ClientPlayNetworking.send(AssetNetwork.REQUEST, buf);
        }
    }

    private static void onChunk(AssetNetwork.Chunk chunk) {
        if (chunk.generation() != generation) return;
        Incoming incoming = INCOMING.get(chunk.path());
        if (incoming == null) return;
        byte[] slice = chunk.data();
        if (incoming.data == null) incoming.data = new byte[incoming.listed.size()];
        // Chunks arrive in order; anything else means the two sides disagree, so the download is abandoned.
        if (chunk.offset() != incoming.received || slice.length > incoming.data.length - incoming.received) {
            abandon("out of order");
            return;
        }
        System.arraycopy(slice, 0, incoming.data, incoming.received, slice.length);
        incoming.received += slice.length;
        if (incoming.received < incoming.data.length) return;
        INCOMING.remove(chunk.path());
        String hash = incoming.listed.hash();
        if (!PackTextures.sha1(incoming.data).equals(hash)) {
            abandon("damaged file " + chunk.path());
            return;
        }
        try {
            Path file = cacheFile(hash);
            Files.createDirectories(file.getParent());
            Files.write(file, incoming.data);
        } catch (IOException e) {
            abandon(e.toString());
            return;
        }
        if (INCOMING.isEmpty()) finish();
    }

    private static void abandon(String why) {
        Apoli.LOGGER.warn("[Apoli] Data pack assets were not applied: {}.", why);
        INCOMING.clear();
        pending = List.of();
    }

    private static void finish() {
        Map<ResourceLocation, Path> files = new HashMap<>();
        for (AssetNetwork.Listed f : pending) {
            ResourceLocation id = id(f.path());
            if (id != null) files.put(id, cacheFile(f.hash()));
        }
        String hash = pendingHash;
        List<AssetNetwork.Listed> list = pending;
        pending = List.of();
        apply(files, hash, overridesOthers(files), list);
    }

    // Whether the pack changes anything that exists without it: a file another pack has, or a language file
    // (every pack's translations are merged, so any language file can change any text).
    private static boolean overridesOthers(Map<ResourceLocation, Path> files) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        for (ResourceLocation id : files.keySet()) {
            if (id.getPath().startsWith("lang/")) return true;
            for (Resource resource : manager.getResourceStack(id)) {
                if (!PACK_NAME.equals(resource.sourcePackId())) return true;
            }
        }
        return false;
    }

    private static void apply(@Nullable Map<ResourceLocation, Path> files, String hash, boolean overrides,
                              @Nullable List<AssetNetwork.Listed> list) {
        if (hash.equals(activeHash)) return;
        active = files;
        activeHash = hash;
        activeOverrides = overrides;
        // Forget the old pack now; the new one is only remembered once it has loaded without errors.
        remember(null);
        Apoli.LOGGER.info("[Apoli] {} data pack assets; reloading resources.", files == null ? "Removing" : "Applying " + files.size());
        reloadUntilApplied(hash, list, 2);
    }

    // Reloads resources and remembers the pack once that finished without errors. If another reload was already
    // running it finished without this change, so the check is repeated after one more reload.
    private static void reloadUntilApplied(String hash, @Nullable List<AssetNetwork.Listed> list, int attempts) {
        Minecraft client = Minecraft.getInstance();
        client.reloadResourcePacks().thenRun(() -> client.execute(() -> {
            if (!hash.equals(activeHash)) return;
            boolean wanted = active != null;
            boolean loaded = client.getResourcePackRepository().getSelectedIds().contains(PACK_NAME);
            if (wanted != loaded) {
                if (attempts > 1) reloadUntilApplied(hash, list, attempts - 1);
            } else if (list != null) {
                remember(list);
            }
        }));
    }
}
