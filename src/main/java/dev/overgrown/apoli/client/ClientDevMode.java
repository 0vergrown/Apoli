package dev.overgrown.apoli.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ClientDevMode {

    private static volatile boolean enabled;

    private ClientDevMode() {}

    public static boolean enabled() {
        return enabled;
    }

    public static void set(boolean value) {
        enabled = value;
    }

    public static void clear() {
        enabled = false;
    }
}
