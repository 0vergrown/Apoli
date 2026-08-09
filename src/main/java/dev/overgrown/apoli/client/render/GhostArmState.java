package dev.overgrown.apoli.client.render;

public final class GhostArmState {
    private GhostArmState() {}

    private static boolean active;
    private static float alpha = 1.0F;

    public static void begin(float ghostAlpha) {
        active = true;
        alpha = ghostAlpha;
    }

    public static void end() {
        active = false;
        alpha = 1.0F;
    }

    public static boolean isActive() {
        return active;
    }

    public static float alpha() {
        return alpha;
    }
}
