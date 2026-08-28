package dev.overgrown.apoli.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class HandRenderPass {
    private static boolean active;

    private HandRenderPass() {}

    public static void begin() {
        active = true;
    }

    public static void end() {
        active = false;
    }

    public static boolean active() {
        return active;
    }
}
