package dev.overgrown.apoli.effects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.loader.IdWildcards;
import dev.overgrown.apoli.loader.JsonFormatParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.quiltmc.parsers.json.JsonFormat;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CustomEffectLoader {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIR = "effects";

    private CustomEffectLoader() {}

    public static List<CustomEffect> load(ResourceManager resourceManager) {
        List<CustomEffect> loaded = new ArrayList<>();
        JsonFormatParser parser = new JsonFormatParser(DIR);
        for (Map.Entry<ResourceLocation, Pair<JsonFormat, List<Resource>>> entry : parser.getMergedStacks(resourceManager).entrySet()) {
            ResourceLocation id = parser.getFileId(entry.getKey(), entry.getValue().getFirst());
            CustomEffect chosen = null;
            for (Resource resource : entry.getValue().getSecond()) {
                CustomEffect effect = parse(id, resource, entry.getValue().getFirst());
                if (effect != null && (chosen == null || effect.loadingPriority() >= chosen.loadingPriority())) {
                    chosen = effect;
                }
            }
            if (chosen != null) loaded.add(chosen);
        }
        loaded.sort(Comparator.comparing(CustomEffect::id));
        if (!loaded.isEmpty()) {
            LOG.info("[Apoli] Loaded {} custom effect(s).", loaded.size());
        }
        return List.copyOf(loaded);
    }

    private static CustomEffect parse(ResourceLocation id, Resource resource, JsonFormat format) {
        JsonElement json;
        try (org.quiltmc.parsers.json.gson.GsonReader reader = new org.quiltmc.parsers.json.gson.GsonReader(org.quiltmc.parsers.json.JsonReader.create(resource.openAsReader(), format))) {
            json = GSON.fromJson(reader, JsonElement.class);
        } catch (Exception e) {
            LOG.error("[Apoli] Failed to read custom effect {} from pack {}", id, resource.sourcePackId(), e);
            return null;
        }
        return CustomEffect.codec(id).parse(IdWildcards.apply(new Dynamic<>(JsonOps.INSTANCE, json), id))
                .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse custom effect {} from pack {}: {}", id, resource.sourcePackId(), err))
                .orElse(null);
    }
}
