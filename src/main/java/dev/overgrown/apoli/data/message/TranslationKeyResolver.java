package dev.overgrown.apoli.data.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.overgrown.apoli.Apoli;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TranslationKeyResolver {
    private static final Pattern PLACEHOLDER = Pattern.compile("#\\{([^}]+)}");
    private static volatile Map<String, Set<String>> translations = Collections.emptyMap();

    private TranslationKeyResolver() {}

    public static void load() {
        Map<String, Set<String>> map = new HashMap<>();
        for (net.neoforged.neoforgespi.language.IModFileInfo info : ModList.get().getModFiles()) {
            IModFile modFile = info.getFile();
            if (modFile == null) {
                continue;
            }
            Path assets;
            try {
                assets = modFile.findResource("assets");
            } catch (Exception e) {
                continue;
            }
            if (assets == null || !Files.isDirectory(assets)) {
                continue;
            }
            try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(assets)) {
                for (Path namespace : namespaces) {
                    Path langDir = namespace.resolve("lang");
                    if (!Files.isDirectory(langDir)) {
                        continue;
                    }
                    try (DirectoryStream<Path> langFiles = Files.newDirectoryStream(langDir, "*.json")) {
                        for (Path langFile : langFiles) {
                            loadFile(langFile, map);
                        }
                    }
                }
            } catch (Exception e) {
                Apoli.LOGGER.warn("[Apoli] Failed to scan lang files for {}: {}", modFile, e.getMessage());
            }
        }
        translations = Collections.unmodifiableMap(map);
        Apoli.LOGGER.info("[Apoli] Loaded {} translation key(s) for message/speech filters.", map.size());
    }

    private static void loadFile(Path file, Map<String, Set<String>> map) {
        try (InputStream stream = Files.newInputStream(file)) {
            JsonObject json = GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    String value = entry.getValue().getAsString();
                    if (!value.isEmpty()) {
                        map.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>()).add(value);
                    }
                }
            }
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Failed to read language file {}: {}", file, e.getMessage());
        }
    }

    public static String expandPattern(String rawPattern) {
        Matcher matcher = PLACEHOLDER.matcher(rawPattern);
        if (!matcher.find()) {
            return rawPattern;
        }
        StringBuilder builder = new StringBuilder();
        matcher.reset();
        while (matcher.find()) {
            String key = matcher.group(1);
            Set<String> values = translations.get(key);
            if (values == null || values.isEmpty()) {
                matcher.appendReplacement(builder, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            String alternatives = values.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
            matcher.appendReplacement(builder, Matcher.quoteReplacement("(?i:" + alternatives + ")"));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }
}
