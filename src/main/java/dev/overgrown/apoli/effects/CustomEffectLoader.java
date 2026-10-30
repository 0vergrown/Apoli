package dev.overgrown.apoli.effects;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.loader.IdWildcards;
import dev.overgrown.apoli.loader.JsonDataFiles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CustomEffectLoader {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String DIR = "effects";

    private CustomEffectLoader() {}

    public static List<CustomEffect> load(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonDataFiles.Version> versions = JsonDataFiles.loadByPriority(resourceManager, DIR, LOG);
        List<CustomEffect> loaded = new ArrayList<>(versions.size());
        for (Map.Entry<ResourceLocation, JsonDataFiles.Version> entry : versions.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonDataFiles.Version version = entry.getValue();
            CustomEffect.codec(id).parse(IdWildcards.apply(new Dynamic<>(JsonOps.INSTANCE, version.json()), id))
                .resultOrPartial(err -> LOG.error("[Apoli] Failed to parse custom effect {} from pack {}: {}", id, version.source().packId(), err))
                .ifPresent(loaded::add);
        }
        loaded.sort(Comparator.comparing(CustomEffect::id));
        if (!loaded.isEmpty()) {
            LOG.info("[Apoli] Loaded {} custom effect(s).", loaded.size());
        }
        return List.copyOf(loaded);
    }
}
