package dev.overgrown.apoli.client;

import dev.overgrown.apoli.client.radial.RadialMenuScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class ScreenCloseKeys {
    private static final int OPEN_WINDOW_TICKS = 30;

    private static @Nullable KeyMapping expected;
    private static int expectedTicks;
    private static @Nullable Screen screen;
    private static @Nullable KeyMapping key;
    private static boolean armed;

    private ScreenCloseKeys() {}

    public static void expect(KeyMapping opener) {
        expected = opener;
        expectedTicks = OPEN_WINDOW_TICKS;
    }

    public static void carryOver() {
        if (screen != null && key != null) expect(key);
    }

    public static void tick(Minecraft mc) {
        Screen current = mc.screen;
        if (screen != null && current != screen) {
            screen = null;
            key = null;
        }
        if (expected != null) {
            if (screen == null && current != null && bindable(current)) {
                screen = current;
                key = expected;
                armed = !ApoliKeyMappings.isHeld(expected);
                expected = null;
            } else if (--expectedTicks <= 0) {
                expected = null;
            }
        }
        if (screen != null && !armed && key != null && !ApoliKeyMappings.isHeld(key)) armed = true;
    }

    public static boolean closes(Screen target, int keyCode, int scanCode) {
        return target == screen && armed && key != null && key.matches(keyCode, scanCode);
    }

    public static boolean isSlotKey(Options options, int keyCode, int scanCode) {
        for (KeyMapping hotbar : options.keyHotbarSlots) {
            if (hotbar.matches(keyCode, scanCode)) return true;
        }
        return options.keySwapOffhand.matches(keyCode, scanCode)
            || options.keyDrop.matches(keyCode, scanCode)
            || options.keyPickItem.matches(keyCode, scanCode);
    }

    public static void clear() {
        expected = null;
        screen = null;
        key = null;
        armed = false;
    }

    private static boolean bindable(Screen candidate) {
        if (candidate instanceof RadialMenuScreen) return true;
        return candidate instanceof AbstractContainerScreen<?>
            && !(candidate instanceof InventoryScreen)
            && !(candidate instanceof CreativeModeInventoryScreen);
    }
}
