package dev.overgrown.apoli.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * Translation-key -> {@link KeyMapping} lookup over {@code Options.keyMappings}.
 * All Apoli keybinds (including {@code key.apoli.primary_active} and
 * {@code key.apoli.secondary_active}) are now data-driven via
 * {@code data/<namespace>/keybinds/<id>.json} and registered at runtime by
 * {@link DynamicKeyMappingManager}.
 */
public final class ApoliKeyMappings {
    private ApoliKeyMappings() {}

    public static @Nullable KeyMapping resolve(String translationKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return null;
        for (KeyMapping km : mc.options.keyMappings) {
            if (km.getName().equals(translationKey)) return km;
        }
        return null;
    }
}
