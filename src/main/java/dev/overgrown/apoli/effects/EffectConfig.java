package dev.overgrown.apoli.effects;

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
import java.util.Optional;

public record EffectConfig (boolean enabled) {
    private static final Gson PRINTER = new GsonBuilder().setPrettyPrinting().create();
    private static final EffectConfig DEFAULTS = new EffectConfig(true);

    private static final Codec<EffectConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("enabled").forGetter(c -> Optional.of(c.enabled))
    ).apply(i, opt -> new EffectConfig(opt.orElse(true))));

    private static volatile EffectConfig instance;

    public static EffectConfig get() {
        EffectConfig local = instance;
        if (local == null) {
            synchronized (EffectConfig.class) {
                local = instance;
                if (local == null) instance = local = load();
            }
        }
        return local;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("apoli-effects.json");
    }

    private static EffectConfig load() {
        Path path = path();
        if (Files.exists(path)) {
            try {
                JsonElement json = JsonParser.parseString(Files.readString(path));
                EffectConfig loaded = CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> Apoli.LOGGER.warn("[Apoli] apoli-effects.json: {}", err))
                        .orElse(null);
                if (loaded != null) return loaded;
            } catch (Exception e) {
                Apoli.LOGGER.warn("[Apoli] Could not read apoli-effects.json; using defaults", e);
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
                    .resultOrPartial(err -> Apoli.LOGGER.warn("[Apoli] apoli-effects.json: {}", err))
                    .orElse(null);
            if (json != null) Files.writeString(path, PRINTER.toJson(json));
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Could not write apoli-effects.json", e);
        }
    }

}