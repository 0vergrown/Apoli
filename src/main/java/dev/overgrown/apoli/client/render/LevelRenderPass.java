package dev.overgrown.apoli.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class LevelRenderPass {
    private static boolean active;

    private LevelRenderPass() {}

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
