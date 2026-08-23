package dev.overgrown.apoli.script;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ApoliScriptConfig {
    private static final com.google.gson.Gson PRINTER = new GsonBuilder().setPrettyPrinting().create();

    private static final Codec<ApoliScriptConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.BOOL.optionalFieldOf("allowDataPackScripts", false).forGetter(c -> c.allowDataPackScripts),
        Codec.INT.optionalFieldOf("instructionBudget", 2_000_000).forGetter(c -> c.instructionBudget),
        Codec.INT.optionalFieldOf("scriptTimeoutMillis", 50).forGetter(c -> c.scriptTimeoutMillis)
    ).apply(i, (allow, budget, timeout) -> {
        ApoliScriptConfig config = new ApoliScriptConfig();
        config.allowDataPackScripts = allow;
        config.instructionBudget = budget;
        config.scriptTimeoutMillis = timeout;
        return config;
    }));

    private static ApoliScriptConfig instance;

    private boolean allowDataPackScripts = false;
    private int instructionBudget = 2_000_000;
    private int scriptTimeoutMillis = 50;

    public boolean allowDataPackScripts() {
        return allowDataPackScripts;
    }

    public int instructionBudget() {
        return Math.max(10_000, instructionBudget);
    }

    public int scriptTimeoutMillis() {
        return Math.max(1, scriptTimeoutMillis);
    }

    public static ApoliScriptConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("apoli-scripts.json");
    }

    private static ApoliScriptConfig load() {
        Path file = path();
        if (Files.exists(file)) {
            try {
                JsonElement json = JsonParser.parseString(Files.readString(file));
                ApoliScriptConfig loaded = CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> Apoli.LOGGER.warn("[Apoli] apoli-scripts.json: {}", err))
                    .orElse(null);
                if (loaded != null) return loaded;
            } catch (Exception e) {
                Apoli.LOGGER.warn("[Apoli] Could not read apoli-scripts.json; using defaults", e);
            }
        }
        ApoliScriptConfig fresh = new ApoliScriptConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Path file = path();
            Files.createDirectories(file.getParent());
            JsonElement json = CODEC.encodeStart(JsonOps.INSTANCE, this)
                .resultOrPartial(err -> Apoli.LOGGER.warn("[Apoli] apoli-scripts.json: {}", err))
                .orElse(null);
            if (json != null) Files.writeString(file, PRINTER.toJson(json));
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Could not write apoli-scripts.json", e);
        }
    }
}
