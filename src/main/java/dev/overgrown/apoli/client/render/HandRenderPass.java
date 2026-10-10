package dev.overgrown.apoli.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class HandRenderPass {
    private static boolean active;
    private static boolean edited;

    private HandRenderPass() {}

    public static void begin() {
        active = true;
        edited = false;
    }

    public static void end() {
        active = false;
        edited = false;
    }

    public static boolean active() {
        return active;
    }

    public static void markEdited() {
        edited = true;
    }

    public static boolean edited() {
        return edited;
    }
}
