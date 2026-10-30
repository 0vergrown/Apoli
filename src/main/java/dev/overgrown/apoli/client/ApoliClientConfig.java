package dev.overgrown.apoli.client;

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

public final class ApoliClientConfig {
    private static final com.google.gson.Gson PRINTER = new GsonBuilder().setPrettyPrinting().create();

    private static final int OFFSET_LIMIT = 512;

    private static final Codec<ApoliClientConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.BOOL.optionalFieldOf("hudAutoStack", true).forGetter(c -> c.hudAutoStack),
        Codec.INT.optionalFieldOf("hudOffsetX", 0).forGetter(ApoliClientConfig::hudOffsetX),
        Codec.INT.optionalFieldOf("hudOffsetY", 0).forGetter(ApoliClientConfig::hudOffsetY)
    ).apply(i, (autoStack, offsetX, offsetY) -> {
        ApoliClientConfig config = new ApoliClientConfig();
        config.hudAutoStack = autoStack;
        config.hudOffsetX = clampOffset(offsetX);
        config.hudOffsetY = clampOffset(offsetY);
        return config;
    }));

    private static ApoliClientConfig instance;

    private boolean hudAutoStack = true;
    private int hudOffsetX = 0;
    private int hudOffsetY = 0;

    public boolean hudAutoStack() {
        return hudAutoStack;
    }

    public void setHudAutoStack(boolean value) {
        hudAutoStack = value;
        save();
    }

    public int hudOffsetX() {
        return hudOffsetX;
    }

    public void setHudOffsetX(int value) {
        hudOffsetX = clampOffset(value);
        save();
    }

    public int hudOffsetY() {
        return hudOffsetY;
    }

    public void setHudOffsetY(int value) {
        hudOffsetY = clampOffset(value);
        save();
    }

    private static int clampOffset(int value) {
        return Math.max(-OFFSET_LIMIT, Math.min(OFFSET_LIMIT, value));
    }

    public static ApoliClientConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("apoli-client.json");
    }

    private static ApoliClientConfig load() {
        Path path = path();
        if (Files.exists(path)) {
            try {
                JsonElement json = JsonParser.parseString(Files.readString(path));
                ApoliClientConfig loaded = CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> Apoli.LOGGER.warn("[Apoli] apoli-client.json: {}", err))
                    .orElse(null);
                if (loaded != null) return loaded;
            } catch (Exception e) {
                Apoli.LOGGER.warn("[Apoli] Could not read apoli-client.json; using defaults", e);
            }
        }
        ApoliClientConfig fresh = new ApoliClientConfig();
        fresh.save();
        return fresh;
    }

    private void save() {
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            JsonElement json = CODEC.encodeStart(JsonOps.INSTANCE, this)
                .resultOrPartial(err -> Apoli.LOGGER.warn("[Apoli] apoli-client.json: {}", err))
                .orElse(null);
            if (json != null) Files.writeString(path, PRINTER.toJson(json));
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Could not write apoli-client.json", e);
        }
    }
}
