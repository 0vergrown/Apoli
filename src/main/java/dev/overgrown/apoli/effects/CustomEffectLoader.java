package dev.overgrown.apoli.effects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.codec.ApoliOps;
import dev.overgrown.apoli.loader.IdWildcards;
import dev.overgrown.apoli.loader.JsonFormatParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.quiltmc.parsers.json.JsonFormat;
import org.slf4j.Logger;

import java.util.*;

public final class CustomEffectLoader {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIR = "effects";

    private CustomEffectLoader() {}

    public static List<CustomEffect> load(ResourceManager resourceManager) {
        List<CustomEffect> loaded = new ArrayList<>();
        JsonFormatParser parser = new JsonFormatParser(DIR);
        for (Map.Entry<ResourceLocation, List<Pair<JsonFormat, Resource>>> entry : parser.getMergedStacks(resourceManager).entrySet()) {
            CustomEffect chosen = null;
            for (Pair<JsonFormat, Resource> pair : entry.getValue()) {
                ResourceLocation id = entry.getKey();
                CustomEffect effect = parse(id, pair.getSecond(), pair.getFirst());
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
        return CustomEffect.codec(id).parse(IdWildcards.apply(new Dynamic<>(ApoliOps.of(JsonOps.INSTANCE), json), id))
                .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse custom effect {} from pack {}: {}", id, resource.sourcePackId(), err))
                .orElse(null);
    }
}
