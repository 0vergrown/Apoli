package dev.overgrown.apoli.power;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ApoliPowers {
    private static volatile Map<ResourceLocation, Power> POWERS = Map.of();

    private ApoliPowers() {}

    public static @Nullable Power get(ResourceLocation id) {
        return POWERS.get(id);
    }

    public static Map<ResourceLocation, Power> view() {
        return Collections.unmodifiableMap(POWERS);
    }

    public static void replaceAll(Map<ResourceLocation, Power> loaded) {
        POWERS = Map.copyOf(loaded);
    }
}