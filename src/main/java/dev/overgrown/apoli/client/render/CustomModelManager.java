package dev.overgrown.apoli.client.render;

import com.google.gson.JsonObject;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.client.model.BedrockModelParser;
import dev.overgrown.apoli.client.model.CustomModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class CustomModelManager implements ResourceManagerReloadListener {
    public static final CustomModelManager INSTANCE = new CustomModelManager();

    private static final String[] PREFIXES = {"geo/", "models/apoli/"};
    private static final String SUFFIX = ".geo.json";
    private static final Map<ResourceLocation, CustomModel> MODELS = new HashMap<>();

    private CustomModelManager() {}

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        MODELS.clear();
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
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), trimmed);
                if (MODELS.containsKey(id)) {
                    continue;
                }
                try (InputStream stream = entry.getValue().open()) {
                    JsonObject json = GsonHelper.parse(new InputStreamReader(stream));
                    MODELS.put(id, BedrockModelParser.parse(id, json));
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
}
