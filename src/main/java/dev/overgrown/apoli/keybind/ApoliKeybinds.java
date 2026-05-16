package dev.overgrown.apoli.keybind;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApoliKeybinds {
    private static volatile Map<ResourceLocation, Keybind> LOADED = Map.of();

    private ApoliKeybinds() {}

    public static void replaceAll(Map<ResourceLocation, Keybind> next) {
        LOADED = Map.copyOf(next);
    }

    public static Map<ResourceLocation, Keybind> view() {
        return Collections.unmodifiableMap(LOADED);
    }

    public static @Nullable Keybind get(ResourceLocation id) {
        return LOADED.get(id);
    }

    public static Map<ResourceLocation, Keybind> snapshot() {
        return new LinkedHashMap<>(LOADED);
    }
}
