package dev.overgrown.apoli.loader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.parsers.json.JsonFormat;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class JsonFormatParser {
    private final FileToIdConverter JSON;
    private final FileToIdConverter JSON5;
    private final FileToIdConverter JSONC;

    public JsonFormatParser(String dir) {
        JSON = new FileToIdConverter(dir, ".json");
        JSON5 = new FileToIdConverter(dir, ".json5");
        JSONC = new FileToIdConverter(dir, ".jsonc");
    }

    public static @NotNull Map<ResourceLocation, JsonElement> apply(ResourceManager resourceManager, String dir, Gson gson, Logger log) {
        Map<ResourceLocation, JsonElement> result = new HashMap<>();

        FileToIdConverter jsonConverter = new FileToIdConverter(dir, ".json");
        FileToIdConverter json5Converter = new FileToIdConverter(dir, ".json5");
        FileToIdConverter jsoncConverter = new FileToIdConverter(dir, ".jsonc");

        for (Map.Entry<ResourceLocation, Resource> entry : jsonConverter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            ResourceLocation resourceLocation2 = jsonConverter.fileToId(resourceLocation);

            try (org.quiltmc.parsers.json.gson.GsonReader reader = new org.quiltmc.parsers.json.gson.GsonReader(org.quiltmc.parsers.json.JsonReader.create(entry.getValue().openAsReader(), JsonFormat.JSON))) {
                JsonElement parsed = gson.fromJson(reader, JsonElement.class);

                JsonElement putResult = result.put(resourceLocation2, parsed);
                if (putResult != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + resourceLocation2);
                }
            } catch (JsonParseException | IllegalArgumentException | IOException exception) {
                log.error("Couldn't parse data file {} from {}", resourceLocation2, resourceLocation, exception);
            }
        }

        for (Map.Entry<ResourceLocation, Resource> entry : json5Converter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            ResourceLocation resourceLocation2 = json5Converter.fileToId(resourceLocation);

            try (org.quiltmc.parsers.json.gson.GsonReader reader = new org.quiltmc.parsers.json.gson.GsonReader(org.quiltmc.parsers.json.JsonReader.create(entry.getValue().openAsReader(), JsonFormat.JSON5))) {
                JsonElement parsed = gson.fromJson(reader, JsonElement.class);

                JsonElement putResult = result.put(resourceLocation2, parsed);
                if (putResult != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + resourceLocation2);
                }
            } catch (JsonParseException | IllegalArgumentException | IOException exception) {
                log.error("Couldn't parse data file {} from {}", resourceLocation2, resourceLocation, exception);
            }
        }

        for (Map.Entry<ResourceLocation, Resource> entry : jsoncConverter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            ResourceLocation resourceLocation2 = jsoncConverter.fileToId(resourceLocation);

            try (org.quiltmc.parsers.json.gson.GsonReader reader = new org.quiltmc.parsers.json.gson.GsonReader(org.quiltmc.parsers.json.JsonReader.create(entry.getValue().openAsReader(), JsonFormat.JSONC))) {
                JsonElement parsed = gson.fromJson(reader, JsonElement.class);

                JsonElement putResult = result.put(resourceLocation2, parsed);
                if (putResult != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + resourceLocation2);
                }
            } catch (JsonParseException | IllegalArgumentException | IOException exception) {
                log.error("Couldn't parse data file {} from {}", resourceLocation2, resourceLocation, exception);
            }
        }

        return result;
    }

    public Map<ResourceLocation, List<Pair<JsonFormat, Resource>>> getMergedStacks(ResourceManager resourceManager) {
        Map<ResourceLocation, List<Resource>> json = JSON.listMatchingResourceStacks(resourceManager);
        Map<ResourceLocation, List<Resource>> jsonc = JSONC.listMatchingResourceStacks(resourceManager);
        Map<ResourceLocation, List<Resource>> json5 = JSON5.listMatchingResourceStacks(resourceManager);

        Map<ResourceLocation, List<Pair<JsonFormat, Resource>>> result = new HashMap<>();

        for (var entry : json.entrySet()) {
            result.put(JSON.fileToId(entry.getKey()), entry.getValue().stream().map(e -> new Pair<>(JsonFormat.JSON, e)).collect(Collectors.toCollection(ArrayList::new)));
        }

        for (var entry : jsonc.entrySet()) {
            var abs = result.putIfAbsent(JSONC.fileToId(entry.getKey()), entry.getValue().stream().map(e -> new Pair<>(JsonFormat.JSONC, e)).collect(Collectors.toCollection(ArrayList::new)));
            if (abs != null) {
                result.get(JSONC.fileToId(entry.getKey())).addAll(entry.getValue().stream().map(e -> new Pair<>(JsonFormat.JSONC, e)).toList());
            }
        }

        for (var entry : json5.entrySet()) {
            var abs = result.putIfAbsent(JSON5.fileToId(entry.getKey()), entry.getValue().stream().map(e -> new Pair<>(JsonFormat.JSON5, e)).collect(Collectors.toCollection(ArrayList::new)));
            if (abs != null) {
                result.get(JSON5.fileToId(entry.getKey())).addAll(entry.getValue().stream().map(e -> new Pair<>(JsonFormat.JSON5, e)).toList());
            }
        }

        return result;
    }
}
