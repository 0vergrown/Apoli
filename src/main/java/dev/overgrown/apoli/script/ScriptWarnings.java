package dev.overgrown.apoli.script;

import dev.overgrown.apoli.Apoli;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class ScriptWarnings {
    private ScriptWarnings() {}

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    static void missing(ApoliScripts.Kind kind, ResourceLocation id) {
        if (!WARNED.add("missing:" + kind + ":" + id)) return;
        Apoli.LOGGER.warn("[Apoli] No {} script registered for '{}'. "
            + "Register one from KubeJS (ApoliEvents) or ship data/<namespace>/apoli/scripts/<path>.js.", kind, id);
    }

    static void failed(ApoliScripts.Kind kind, ResourceLocation id, Throwable error) {
        if (!WARNED.add("failed:" + kind + ":" + id)) return;
        Apoli.LOGGER.error("[Apoli] {} script '{}' threw; it will keep running but this is logged once.", kind, id, error);
    }

    static void reset() {
        WARNED.clear();
    }
}
