package dev.overgrown.apoli.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public final class ApoliKeyMappings {
    private static final Map<KeyMapping, Boolean> WAS_DOWN = new HashMap<>();

    private static final Map<KeyMapping, KeyState> TICK_CACHE = new HashMap<>();

    private ApoliKeyMappings() {}

    private record KeyState(boolean down, boolean tap) {}

    private enum Probe { DOWN, UP, UNKNOWN }

    public static void beginTick() {
        TICK_CACHE.clear();
    }

    public static @Nullable KeyMapping resolve(String translationKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return null;
        for (KeyMapping km : mc.options.keyMappings) {
            if (km.getName().equals(translationKey)) return km;
        }
        return null;
    }

    public static boolean consumePress(KeyMapping km, boolean continuous) {
        KeyState st = state(km);
        if (screenOpen()) return false;
        return continuous ? st.down() : st.tap();
    }

    public static boolean isDown(KeyMapping km) {
        KeyState st = state(km);
        return !screenOpen() && st.down();
    }

    private static KeyState state(KeyMapping km) {
        KeyState st = TICK_CACHE.get(km);
        if (st == null) {
            st = computeState(km);
            TICK_CACHE.put(km, st);
        }
        return st;
    }

    private static KeyState computeState(KeyMapping km) {
        boolean wasDown = WAS_DOWN.getOrDefault(km, false);
        boolean down = switch (probe(km)) {
            case DOWN -> true;
            case UP -> false;
            case UNKNOWN -> wasDown;
        };
        WAS_DOWN.put(km, down);
        boolean clicked = false;
        while (km.consumeClick()) clicked = true;
        boolean tap = !wasDown && (down || clicked);
        return new KeyState(down, tap);
    }

    private static boolean screenOpen() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.screen != null;
    }

    private static Probe probe(KeyMapping km) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return Probe.UNKNOWN;
        InputConstants.Key key;
        try {
            key = InputConstants.getKey(km.saveString());
        } catch (Exception e) {
            return vanillaProbe(km);
        }
        int value = key.getValue();
        if (value < 0) return Probe.UP;
        long window = mc.getWindow().getWindow();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, value) ? Probe.DOWN : Probe.UP;
            case MOUSE -> GLFW.glfwGetMouseButton(window, value) == GLFW.GLFW_PRESS ? Probe.DOWN : Probe.UP;
            default -> vanillaProbe(km);
        };
    }

    private static Probe vanillaProbe(KeyMapping km) {
        return km.isDown() ? Probe.DOWN : Probe.UP;
    }
}
