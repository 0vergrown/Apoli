package dev.overgrown.apoli.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class KeyReleases {
    private static final Map<ResourceLocation, KeyMapping> PRESSED = new LinkedHashMap<>();

    private static Consumer<ResourceLocation> sender = power -> {};

    private KeyReleases() {}

    public static void setSender(Consumer<ResourceLocation> next) {
        sender = next;
    }

    public static void pressed(ResourceLocation powerId, KeyMapping key) {
        PRESSED.putIfAbsent(powerId, key);
    }

    public static void tick() {
        if (PRESSED.isEmpty()) return;
        for (Iterator<Map.Entry<ResourceLocation, KeyMapping>> it = PRESSED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ResourceLocation, KeyMapping> entry = it.next();
            KeyMapping key = entry.getValue();
            if (ApoliKeyMappings.isDown(key) || ForcedKeys.isForced(key)) continue;
            it.remove();
            sender.accept(entry.getKey());
        }
    }

    public static void clear() {
        PRESSED.clear();
    }
}
