package dev.overgrown.apoli.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.client.model.BedrockModelParser;
import dev.overgrown.apoli.client.model.CustomModel;
import dev.overgrown.apoli.client.model.TextureMask;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower.GeometryRender;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CustomModelManager implements SimpleSynchronousResourceReloadListener {
    public static final CustomModelManager INSTANCE = new CustomModelManager();

    private static final String[] PREFIXES = {"geo/", "models/apoli/"};
    private static final String SUFFIX = ".geo.json";
    private static final Map<ResourceLocation, CustomModel> MODELS = new HashMap<>();
    private static final Map<ResourceLocation, CustomModel> UNBOUND = new HashMap<>();
    private static final Map<ResourceLocation, BedrockModelParser.Parsed> MESH_SOURCES = new HashMap<>();
    private static final Map<ResourceLocation, Map<ResourceLocation, CustomModel[]>> MESHED = new HashMap<>();
    private static final Map<ResourceLocation, TextureMask> MASKS = new HashMap<>();

    private CustomModelManager() {}

    @Override
    public ResourceLocation getFabricId() {
        return Apoli.id("custom_models");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        MODELS.clear();
        UNBOUND.clear();
        synchronized (CustomModelManager.class) {
            MESH_SOURCES.clear();
            MESHED.clear();
            MASKS.clear();
        }
        for (String prefix : PREFIXES) {
            String scanPath = prefix.substring(0, prefix.length() - 1);
            Map<ResourceLocation, Resource> found = manager.listResources(
                scanPath, location -> location.getPath().endsWith(SUFFIX));
            for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
                ResourceLocation file = entry.getKey();
                String path = file.getPath();
                if (!path.startsWith(prefix)) {
                    continue;
                }
                String trimmed = path.substring(prefix.length(), path.length() - SUFFIX.length());
                ResourceLocation id = new ResourceLocation(file.getNamespace(), trimmed);
                if (MODELS.containsKey(id)) {
                    continue;
                }
                try (InputStream stream = entry.getValue().open()) {
                    JsonElement raw = JsonParser.parseReader(new InputStreamReader(stream));
                    Dynamic<JsonElement> json = new Dynamic<>(JsonOps.INSTANCE, raw);
                    BedrockModelParser.Parsed parsed = BedrockModelParser.parse(id, json);
                    MODELS.put(id, parsed.bound());
                    if (parsed.unbound() != parsed.bound()) {
                        UNBOUND.put(id, parsed.unbound());
                    }
                    if (parsed.textureMeshes()) {
                        synchronized (CustomModelManager.class) {
                            MESH_SOURCES.put(id, parsed);
                        }
                    }
                } catch (Exception e) {
                    Apoli.LOGGER.error("[Apoli] Failed to load custom model {}: {}", id, e.getMessage());
                }
            }
        }
        Apoli.LOGGER.info("[Apoli] Loaded {} custom model(s) for custom_model_render.", MODELS.size());
    }

    @Nullable
    public static CustomModel get(ResourceLocation id) {
        return MODELS.get(id);
    }

    @Nullable
    public static CustomModel get(ResourceLocation id, boolean bindBodyParts) {
        if (!bindBodyParts) {
            CustomModel unbound = UNBOUND.get(id);
            if (unbound != null) {
                return unbound;
            }
        }
        return MODELS.get(id);
    }

    @Nullable
    public static CustomModel get(GeometryRender render, boolean bindBodyParts, @Nullable Entity subject) {
        CustomModel base = get(render.model(), bindBodyParts);
        if (base == null || !base.textureMeshes()) {
            return base;
        }
        ResourceLocation texture = DynamicTextures.resolve(render.texture(), subject);
        if (texture == null) {
            return base;
        }
        CustomModel[] variants = meshed(render.model(), texture);
        if (variants == null) {
            return base;
        }
        return bindBodyParts ? variants[0] : variants[1];
    }

    @Nullable
    private static synchronized CustomModel[] meshed(ResourceLocation model, ResourceLocation texture) {
        Map<ResourceLocation, CustomModel[]> byTexture = MESHED.computeIfAbsent(model, key -> new HashMap<>(4));
        CustomModel[] found = byTexture.get(texture);
        if (found != null) {
            return found;
        }
        BedrockModelParser.Parsed parsed = MESH_SOURCES.get(model);
        if (parsed == null) {
            return null;
        }
        TextureMask mask = MASKS.get(texture);
        if (mask == null) {
            mask = readMask(texture);
            MASKS.put(texture, mask);
        }
        found = mask.isEmpty()
            ? new CustomModel[]{parsed.bound(), parsed.unbound()}
            : parsed.variants(mask);
        byTexture.put(texture, found);
        return found;
    }

    private static TextureMask readMask(ResourceLocation texture) {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(texture);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                return TextureMask.of(image);
            } catch (IOException | RuntimeException e) {
                Apoli.LOGGER.warn("[Apoli] Couldn't read {} to build texture meshes: {}", texture, e.getMessage());
                return TextureMask.NONE;
            }
        }
        AbstractTexture loaded = minecraft.getTextureManager().getTexture(texture, null);
        if (loaded instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
            return TextureMask.of(dynamic.getPixels());
        }
        Apoli.LOGGER.warn("[Apoli] Texture meshes are cut from the model's texture, and {} is neither a resource nor a dynamic texture, so they are left out.", texture);
        return TextureMask.NONE;
    }
}
