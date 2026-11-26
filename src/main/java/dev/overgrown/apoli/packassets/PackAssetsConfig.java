package dev.overgrown.apoli.packassets;

import com.google.gson.Gson;
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

// config/apoli-pack-assets.json: turns data pack textures and data pack assets on or off on the server. Read once;
// a missing file is written with the defaults.
public record PackAssetsConfig(boolean dataPackTextures, boolean dataPackAssets) {
    private static final Gson PRINTER = new GsonBuilder().setPrettyPrinting().create();
    private static final PackAssetsConfig DEFAULTS = new PackAssetsConfig(true, true);

    private static final Codec<PackAssetsConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.BOOL.optionalFieldOf("data_pack_textures", true).forGetter(PackAssetsConfig::dataPackTextures),
        Codec.BOOL.optionalFieldOf("data_pack_assets", true).forGetter(PackAssetsConfig::dataPackAssets)
    ).apply(i, PackAssetsConfig::new));

    private static volatile PackAssetsConfig instance;

    public static PackAssetsConfig get() {
        PackAssetsConfig local = instance;
        if (local == null) {
            synchronized (PackAssetsConfig.class) {
                local = instance;
                if (local == null) instance = local = load();
            }
        }
        return local;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("apoli-pack-assets.json");
    }

    private static PackAssetsConfig load() {
        Path path = path();
        if (Files.exists(path)) {
            try {
                JsonElement json = JsonParser.parseString(Files.readString(path));
                PackAssetsConfig loaded = CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> Apoli.LOGGER.warn("[Apoli] apoli-pack-assets.json: {}", err))
                    .orElse(null);
                if (loaded != null) return loaded;
            } catch (Exception e) {
                Apoli.LOGGER.warn("[Apoli] Could not read apoli-pack-assets.json; using defaults", e);
            }
            return DEFAULTS;
        }
        DEFAULTS.save();
        return DEFAULTS;
    }

    private void save() {
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            JsonElement json = CODEC.encodeStart(JsonOps.INSTANCE, this)
                .resultOrPartial(err -> Apoli.LOGGER.warn("[Apoli] apoli-pack-assets.json: {}", err))
                .orElse(null);
            if (json != null) Files.writeString(path, PRINTER.toJson(json));
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Could not write apoli-pack-assets.json", e);
        }
    }
}
