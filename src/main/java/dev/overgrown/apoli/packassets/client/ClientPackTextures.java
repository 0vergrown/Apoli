package dev.overgrown.apoli.packassets.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.packassets.PackTextures;
import dev.overgrown.apoli.packassets.TextureNetwork;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Client side of data pack textures. Files are taken from a disk cache when the same file was downloaded before, and
// requested otherwise. A texture a resource pack already has is left to the resource pack.
@Environment(EnvType.CLIENT)
public final class ClientPackTextures {
    private ClientPackTextures() {}

    private static final int MAX_DIMENSION = 4096;
    // Most pixels all registered textures may add up to (about 190 MB of video memory).
    private static final long MAX_PIXELS = 48L * 1024 * 1024;
    private static long pixels;
    private static final Map<ResourceLocation, Long> PIXELS_OF = new HashMap<>();

    private static final class Incoming {
        final TextureNetwork.Listed listed;
        byte[] data;
        int received;

        Incoming(TextureNetwork.Listed listed) {
            this.listed = listed;
        }
    }

    private static int generation = -1;
    // Textures registered here, with the hash of the file each came from.
    private static final Map<ResourceLocation, String> REGISTERED = new HashMap<>();
    private static final Map<ResourceLocation, String> WANTED = new HashMap<>();
    private static final Map<ResourceLocation, Incoming> INCOMING = new HashMap<>();

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(TextureNetwork.Manifest.TYPE, (manifest, context) -> onManifest(manifest));
        ClientPlayNetworking.registerGlobalReceiver(TextureNetwork.Chunk.TYPE, (chunk, context) -> onChunk(chunk));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ClientPackTextures::clear));
    }

    private static Path cacheFile(String hash) {
        return FabricLoader.getInstance().getGameDir().resolve("apoli-texture-cache").resolve(hash + ".png");
    }

    private static boolean acceptable(TextureNetwork.Listed t) {
        String path = t.id().getPath();
        return path.startsWith("textures/") && path.endsWith(".png") && t.size() > 0 && t.size() <= PackTextures.MAX_FILE
            && t.id().toString().length() <= PackTextures.MAX_ID_LENGTH && PackTextures.hex40(t.hash());
    }

    private static void onManifest(TextureNetwork.Manifest manifest) {
        Minecraft mc = Minecraft.getInstance();
        generation = manifest.generation();
        INCOMING.clear();
        WANTED.clear();
        long total = 0;
        for (TextureNetwork.Listed t : manifest.textures()) {
            if (!acceptable(t)) continue;
            if (total + t.size() > PackTextures.MAX_TOTAL) {
                Apoli.LOGGER.warn("[Apoli] The server lists more than {} MB of data pack textures; the rest are ignored.",
                    PackTextures.MAX_TOTAL / (1024 * 1024));
                break;
            }
            total += t.size();
            WANTED.put(t.id(), t.hash());
        }
        // Drop what the server no longer lists, or lists with different contents.
        for (ResourceLocation id : new ArrayList<>(REGISTERED.keySet())) {
            if (!REGISTERED.get(id).equals(WANTED.get(id))) release(id);
        }
        List<ResourceLocation> missing = new ArrayList<>();
        for (TextureNetwork.Listed t : manifest.textures()) {
            if (!WANTED.containsKey(t.id()) || REGISTERED.containsKey(t.id())) continue;
            if (mc.getResourceManager().getResource(t.id()).isPresent() || takenByGame(t.id())) continue;
            byte[] cached = readCache(t);
            if (cached != null) {
                decode(t.id(), t.hash(), cached);
            } else {
                INCOMING.put(t.id(), new Incoming(t));
                missing.add(t.id());
            }
        }
        for (int from = 0; from < missing.size(); from += TextureNetwork.REQUEST_BATCH) {
            ClientPlayNetworking.send(new TextureNetwork.Request(generation, from == 0,
                List.copyOf(missing.subList(from, Math.min(missing.size(), from + TextureNetwork.REQUEST_BATCH)))));
        }
    }

    private static byte[] readCache(TextureNetwork.Listed t) {
        Path file = cacheFile(t.hash());
        try {
            if (!Files.isRegularFile(file) || Files.size(file) != t.size()) return null;
            byte[] data = Files.readAllBytes(file);
            return PackTextures.sha1(data).equals(t.hash()) ? data : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static void onChunk(TextureNetwork.Chunk chunk) {
        if (chunk.generation() != generation) return;
        Incoming incoming = INCOMING.get(chunk.id());
        if (incoming == null) return;
        byte[] slice = chunk.data();
        if (incoming.data == null) incoming.data = new byte[incoming.listed.size()];
        // Chunks arrive in order; anything else means the two sides disagree, so the file is abandoned.
        if (chunk.offset() != incoming.received || slice.length > incoming.data.length - incoming.received) {
            INCOMING.remove(chunk.id());
            return;
        }
        System.arraycopy(slice, 0, incoming.data, incoming.received, slice.length);
        incoming.received += slice.length;
        if (incoming.received < incoming.data.length) return;
        INCOMING.remove(chunk.id());
        String hash = incoming.listed.hash();
        if (!PackTextures.sha1(incoming.data).equals(hash)) {
            Apoli.LOGGER.warn("[Apoli] Data pack texture {} arrived damaged and was dropped.", chunk.id());
            return;
        }
        byte[] data = incoming.data;
        Util.ioPool().execute(() -> {
            try {
                Path file = cacheFile(hash);
                Files.createDirectories(file.getParent());
                Files.write(file, data);
            } catch (IOException e) {
                Apoli.LOGGER.warn("[Apoli] Could not cache data pack texture {}: {}", chunk.id(), e.toString());
            }
        });
        decode(chunk.id(), hash, data);
    }

    // Width and height from the PNG header, or null when it isn't a PNG.
    private static int[] pngSize(byte[] data) {
        if (data.length < 24 || (data[0] & 0xFF) != 0x89 || data[1] != 'P' || data[2] != 'N' || data[3] != 'G') return null;
        int width = ((data[16] & 0xFF) << 24) | ((data[17] & 0xFF) << 16) | ((data[18] & 0xFF) << 8) | (data[19] & 0xFF);
        int height = ((data[20] & 0xFF) << 24) | ((data[21] & 0xFF) << 16) | ((data[22] & 0xFF) << 8) | (data[23] & 0xFF);
        return new int[]{width, height};
    }

    // Decodes off the main thread, then registers the texture on it.
    private static void decode(ResourceLocation id, String hash, byte[] data) {
        int[] size = pngSize(data);
        if (size == null || size[0] < 1 || size[1] < 1 || size[0] > MAX_DIMENSION || size[1] > MAX_DIMENSION) {
            Apoli.LOGGER.warn("[Apoli] Data pack texture {} is not a PNG of at most {}x{} and was skipped.", id, MAX_DIMENSION, MAX_DIMENSION);
            return;
        }
        long area = (long) size[0] * size[1];
        Util.backgroundExecutor().execute(() -> {
            NativeImage image;
            try {
                image = NativeImage.read(new ByteArrayInputStream(data));
            } catch (IOException | RuntimeException e) {
                Apoli.LOGGER.warn("[Apoli] Could not decode data pack texture {}: {}", id, e.toString());
                return;
            }
            Minecraft.getInstance().execute(() -> {
                // The server may have reloaded, or the player left, while this was decoding.
                if (!hash.equals(WANTED.get(id)) || takenByGame(id)) {
                    image.close();
                    return;
                }
                if (pixels + area > MAX_PIXELS) {
                    image.close();
                    Apoli.LOGGER.warn("[Apoli] Data pack textures use too much video memory; {} was skipped.", id);
                    return;
                }
                if (REGISTERED.containsKey(id)) release(id);
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
                REGISTERED.put(id, hash);
                PIXELS_OF.put(id, area);
                pixels += area;
            });
        });
    }

    // Whether the game already has its own texture under this id (an atlas, a font page...). Those are never
    // replaced. A missing-texture placeholder from an earlier failed lookup doesn't count.
    private static boolean takenByGame(ResourceLocation id) {
        if (REGISTERED.containsKey(id)) return false;
        AbstractTexture existing = Minecraft.getInstance().getTextureManager().getTexture(id, null);
        return existing != null && existing != MissingTextureAtlasSprite.getTexture();
    }

    private static void release(ResourceLocation id) {
        Long area = PIXELS_OF.remove(id);
        if (area != null) pixels -= area;
        REGISTERED.remove(id);
        Minecraft.getInstance().getTextureManager().release(id);
    }

    private static void clear() {
        generation = -1;
        WANTED.clear();
        INCOMING.clear();
        for (ResourceLocation id : new ArrayList<>(REGISTERED.keySet())) release(id);
    }
}
