package dev.overgrown.apoli.client.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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
