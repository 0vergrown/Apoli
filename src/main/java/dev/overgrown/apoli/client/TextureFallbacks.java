package dev.overgrown.apoli.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class TextureFallbacks {

    private static final Map<ResourceLocation, Boolean> PRESENT = new HashMap<>();

    private TextureFallbacks() {}

    public static boolean exists(ResourceLocation texture) {
        Boolean present = PRESENT.get(texture);
        if (present == null) {
            present = Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
            PRESENT.put(texture, present);
        }
        return present;
    }

    public static ResourceLocation first(ResourceLocation texture, List<ResourceLocation> fallbacks) {
        if (fallbacks.isEmpty() || exists(texture)) return texture;
        for (int i = 0, n = fallbacks.size(); i < n; i++) {
            ResourceLocation fallback = fallbacks.get(i);
            if (exists(fallback)) return fallback;
        }
        return texture;
    }

    public static void clear() {
        PRESENT.clear();
    }
}
