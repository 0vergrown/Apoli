package dev.overgrown.apoli.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ApoliClientConfig {
    private static final com.google.gson.Gson PRINTER = new GsonBuilder().setPrettyPrinting().create();

    private static final int OFFSET_LIMIT = 512;

    private static final Codec<ApoliClientConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.BOOL.optionalFieldOf("speechToAction", false).forGetter(c -> c.speechToAction),
        Codec.BOOL.optionalFieldOf("speechPushToTalk", true).forGetter(c -> c.speechPushToTalk),
        Codec.BOOL.optionalFieldOf("speechEcho", false).forGetter(c -> c.speechEcho),
        Codec.STRING.optionalFieldOf("speechInputDevice", "").forGetter(c -> c.speechInputDevice),
        Codec.BOOL.optionalFieldOf("speechInstant", true).forGetter(c -> c.speechInstant),
        Codec.STRING.optionalFieldOf("speechSource", "auto").forGetter(ApoliClientConfig::speechSource),
        Codec.BOOL.optionalFieldOf("hudAutoStack", true).forGetter(c -> c.hudAutoStack),
        Codec.INT.optionalFieldOf("hudOffsetX", 0).forGetter(ApoliClientConfig::hudOffsetX),
        Codec.INT.optionalFieldOf("hudOffsetY", 0).forGetter(ApoliClientConfig::hudOffsetY)
    ).apply(i, (toAction, pushToTalk, echo, device, instant, source, autoStack, offsetX, offsetY) -> {
        ApoliClientConfig config = new ApoliClientConfig();
        config.speechToAction = toAction;
        config.speechPushToTalk = pushToTalk;
        config.speechEcho = echo;
        config.speechInputDevice = device;
        config.speechInstant = instant;
        config.speechSource = source;
        config.hudAutoStack = autoStack;
        config.hudOffsetX = clampOffset(offsetX);
        config.hudOffsetY = clampOffset(offsetY);
        return config;
    }));

    private static ApoliClientConfig instance;

    private boolean speechToAction = false;
    private boolean speechPushToTalk = true;
    private boolean speechEcho = false;
    private String speechInputDevice = "";
    private boolean speechInstant = true;

    public boolean speechInstant() {
        return speechInstant;
    }

    public void setSpeechInstant(boolean value) {
        speechInstant = value;
        save();
    }

    private String speechSource = "auto";

    public String speechSource() {
        return speechSource == null || speechSource.isBlank() ? "auto" : speechSource;
    }

    public void setSpeechSource(String value) {
        speechSource = value == null || value.isBlank() ? "auto" : value;
        save();
    }

    public String speechInputDevice() {
        return speechInputDevice == null ? "" : speechInputDevice;
    }

    public void setSpeechInputDevice(String value) {
        speechInputDevice = value == null ? "" : value;
        save();
    }

    public boolean speechEcho() {
        return speechEcho;
    }

    public void setSpeechEcho(boolean value) {
        speechEcho = value;
        save();
    }

    public boolean speechToAction() {
        return speechToAction;
    }

    public void setSpeechToAction(boolean value) {
        speechToAction = value;
        save();
    }

    public boolean speechPushToTalk() {
        return speechPushToTalk;
    }

    public void setSpeechPushToTalk(boolean value) {
        speechPushToTalk = value;
        save();
    }

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
        return FabricLoader.getInstance().getConfigDir().resolve("apoli-client.json");
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
