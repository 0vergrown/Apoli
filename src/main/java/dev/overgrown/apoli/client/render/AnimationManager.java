package dev.overgrown.apoli.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.client.model.BedrockAnimation;
import dev.overgrown.apoli.client.model.BedrockAnimationParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AnimationManager implements SimpleSynchronousResourceReloadListener {
    public static final AnimationManager INSTANCE = new AnimationManager();

    private static final String PREFIX = "animations";
    private static final String[] SUFFIXES = {".animation.json", ".json"};
    private static final Map<ResourceLocation, Map<String, BedrockAnimation>> FILES = new HashMap<>();
    private static final Map<ResourceLocation, Set<String>> WARNED = new HashMap<>();

    private AnimationManager() {}

    @Override
    public ResourceLocation getFabricId() {
        return Apoli.id("custom_model_animations");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        FILES.clear();
        WARNED.clear();
        AnimationPlayer.clearWarnings();
        int loaded = 0;
        Map<ResourceLocation, Resource> found = manager.listResources(
            PREFIX, location -> location.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            ResourceLocation file = entry.getKey();
            ResourceLocation id = strip(file);
            if (id == null || FILES.containsKey(id)) continue;
            try (InputStream stream = entry.getValue().open()) {
                JsonElement raw = JsonParser.parseReader(new InputStreamReader(stream));
                Map<String, BedrockAnimation> parsed =
                    BedrockAnimationParser.parse(id, new Dynamic<>(JsonOps.INSTANCE, raw));
                if (parsed.isEmpty()) continue;
                FILES.put(id, parsed);
                loaded += parsed.size();
            } catch (Exception e) {
                Apoli.LOGGER.error("[Apoli] Failed to load animation file {}: {}", id, e.getMessage());
            }
        }
        Apoli.LOGGER.info("[Apoli] Loaded {} custom model animation(s) from {} file(s).", loaded, FILES.size());
    }

    @Nullable
    private static ResourceLocation strip(ResourceLocation file) {
        String path = file.getPath();
        if (!path.startsWith(PREFIX + "/")) return null;
        String trimmed = path.substring(PREFIX.length() + 1);
        for (String suffix : SUFFIXES) {
            if (trimmed.endsWith(suffix)) {
                return new ResourceLocation(file.getNamespace(),
                    trimmed.substring(0, trimmed.length() - suffix.length()));
            }
        }
        return null;
    }

    @Nullable
    public static BedrockAnimation get(ResourceLocation file, @Nullable String name) {
        Map<String, BedrockAnimation> animations = FILES.get(file);
        if (animations == null || animations.isEmpty()) {
            warnMissing(file, name, null);
            return null;
        }
        if (name == null) return animations.values().iterator().next();
        BedrockAnimation found = animations.get(name);
        if (found == null) warnMissing(file, name, animations);
        return found;
    }

    private static void warnMissing(ResourceLocation file, @Nullable String name,
                                    @Nullable Map<String, BedrockAnimation> available) {
        Set<String> seen = WARNED.computeIfAbsent(file, key -> new HashSet<>(2));
        if (!seen.add(name == null ? "" : name)) return;
        if (available == null) {
            Apoli.LOGGER.warn("[Apoli] No animation file '{}' is loaded, so nothing will play. "
                + "It is expected at assets/{}/animations/{}.animation.json inside an enabled resource pack.",
                file, file.getNamespace(), file.getPath());
            return;
        }
        Apoli.LOGGER.warn("[Apoli] Animation file '{}' has no clip called '{}', so nothing will play. It contains: {}",
            file, name, available.keySet());
    }
}
