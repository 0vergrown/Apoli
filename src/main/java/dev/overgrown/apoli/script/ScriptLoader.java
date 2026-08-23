package dev.overgrown.apoli.script;

import dev.overgrown.apoli.Apoli;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ScriptLoader extends SimplePreparableReloadListener<Map<ResourceLocation, String>> {
    private static final String DIRECTORY = "apoli/scripts";
    private static final String SUFFIX = ".js";
    private static final int MAX_SOURCE_BYTES = 512 * 1024;

    @Override
    protected Map<ResourceLocation, String> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, String> sources = new LinkedHashMap<>();
        if (!ApoliScriptConfig.get().allowDataPackScripts()) return sources;

        for (Map.Entry<ResourceLocation, Resource> entry
            : manager.listResources(DIRECTORY, path -> path.getPath().endsWith(SUFFIX)).entrySet()) {
            ResourceLocation file = entry.getKey();
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                String source = reader.lines().reduce(new StringBuilder(), StringBuilder::append, StringBuilder::append).toString();
                if (source.length() > MAX_SOURCE_BYTES) {
                    Apoli.LOGGER.error("[Apoli] Script {} is larger than {} bytes and was skipped", file, MAX_SOURCE_BYTES);
                    continue;
                }
                sources.put(scriptId(file), source);
            } catch (Exception e) {
                Apoli.LOGGER.error("[Apoli] Could not read script {}", file, e);
            }
        }
        return sources;
    }

    @Override
    protected void apply(Map<ResourceLocation, String> sources, ResourceManager manager, ProfilerFiller profiler) {
        ScriptBackend backend = ApoliScripts.backend();
        if (sources.isEmpty()) return;
        if (backend == null || !backend.available()) {
            Apoli.LOGGER.warn("[Apoli] {} data pack script(s) found but no script backend is installed. "
                + "Install KubeJS to run apoli:script files.", sources.size());
            return;
        }
        backend.beginReload();
        for (Map.Entry<ResourceLocation, String> entry : sources.entrySet()) {
            try {
                backend.load(entry.getKey(), entry.getValue());
            } catch (Throwable t) {
                Apoli.LOGGER.error("[Apoli] Script {} failed to load", entry.getKey(), t);
            }
        }
        backend.endReload();
        Apoli.LOGGER.info("[Apoli] Loaded {} data pack script(s) via {}", sources.size(), backend.name());
    }

    private static ResourceLocation scriptId(ResourceLocation file) {
        String path = file.getPath();
        String trimmed = path.substring(DIRECTORY.length() + 1);
        return ResourceLocation.fromNamespaceAndPath(file.getNamespace(), trimmed);
    }
}
