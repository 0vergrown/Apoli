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
        KeyState st = TICK_CACHE.get(km);
        if (st == null) {
            st = computeState(km);
            TICK_CACHE.put(km, st);
        }
        return continuous ? st.down() : st.tap();
    }

    private static KeyState computeState(KeyMapping km) {
        boolean down = isHeld(km);
        boolean wasDown = WAS_DOWN.getOrDefault(km, false);
        WAS_DOWN.put(km, down);
        boolean clicked = false;
        while (km.consumeClick()) clicked = true;
        boolean tap = (down && !wasDown) || clicked;
        return new KeyState(down, tap);
    }

    private static boolean isHeld(KeyMapping km) {
        if (km.isDown()) return true;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || mc.screen != null) return false;
        InputConstants.Key key;
        try {
            key = InputConstants.getKey(km.saveString());
        } catch (Exception e) {
            return false;
        }
        int value = key.getValue();
        if (value < 0) return false;
        long window = mc.getWindow().getWindow();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, value);
            case MOUSE -> GLFW.glfwGetMouseButton(window, value) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }
}
