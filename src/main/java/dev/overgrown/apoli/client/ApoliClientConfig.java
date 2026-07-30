package dev.overgrown.apoli.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.overgrown.apoli.Apoli;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ApoliClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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
        try {
            if (Files.exists(path)) {
                ApoliClientConfig loaded = GSON.fromJson(Files.readString(path), ApoliClientConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            }
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Failed to read apoli-client.json, using defaults", e);
        }
        ApoliClientConfig fresh = new ApoliClientConfig();
        fresh.save();
        return fresh;
    }

    private void save() {
        try {
            Files.writeString(path(), GSON.toJson(this));
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Failed to write apoli-client.json", e);
        }
    }
}
