package dev.overgrown.apoli.client.render;

import com.google.gson.JsonObject;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.client.model.BedrockModelParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class CustomModelManager implements SimpleSynchronousResourceReloadListener {
    public static final CustomModelManager INSTANCE = new CustomModelManager();

    private static final String[] PREFIXES = {"geo/", "models/apoli/"};
    private static final String SUFFIX = ".geo.json";
    private static final Map<ResourceLocation, LayerDefinition> DEFINITIONS = new HashMap<>();
    private static final Map<ResourceLocation, ModelPart> BAKED = new HashMap<>();

    private CustomModelManager() {}

    @Override
    public ResourceLocation getFabricId() {
        return Apoli.id("custom_models");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        DEFINITIONS.clear();
        BAKED.clear();
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
                if (DEFINITIONS.containsKey(id)) {
                    continue;
                }
                try (InputStream stream = entry.getValue().open()) {
                    JsonObject json = GsonHelper.parse(new InputStreamReader(stream));
                    DEFINITIONS.put(id, BedrockModelParser.parse(json));
                } catch (Exception e) {
                    Apoli.LOGGER.error("[Apoli] Failed to load custom model {}: {}", id, e.getMessage());
                }
            }
        }
        Apoli.LOGGER.info("[Apoli] Loaded {} custom model(s) for custom_model_render.", DEFINITIONS.size());
    }

    @Nullable
    public static ModelPart getBaked(ResourceLocation id) {
        ModelPart cached = BAKED.get(id);
        if (cached != null) {
            return cached;
        }
        LayerDefinition definition = DEFINITIONS.get(id);
        if (definition == null) {
            return null;
        }
        ModelPart baked = definition.bakeRoot();
        BAKED.put(id, baked);
        return baked;
    }
}
