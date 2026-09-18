package dev.overgrown.apoli.client;

import dev.overgrown.apoli.mixin.hud.GuiStatusHelperAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;

@Environment(EnvType.CLIENT)
public final class HudStatusStack {

    public static final int BASE_HEIGHT = 39;
    public static final int ROW_HEIGHT = 10;

    private static final int MAX_ROWS = 12;
    private static final int GRID_TOLERANCE = 2;
    private static final int COLUMN_WIDTH = 96;

    private static boolean armed;
    private static boolean paused;
    private static int screenWidth;
    private static int screenHeight;
    private static int liveMask;
    private static int settledMask;
    private static int reserved;

    private HudStatusStack() {}

    public static void beginFrame(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        settledMask = liveMask;
        liveMask = 0;
        reserved = 0;
        paused = false;
        armed = ApoliClientConfig.get().hudAutoStack();
    }

    public static void endFrame() {
        armed = false;
    }

    public static void pause() {
        paused = true;
    }

    public static void resume() {
        paused = false;
    }

    public static boolean recording() {
        return armed && !paused;
    }

    public static void noteDraw(int x1, int y1, int x2, int y2) {
        int left = Math.min(x1, x2);
        int right = Math.max(x1, x2);
        int centre = screenWidth / 2;
        if (left < centre || right > centre + COLUMN_WIDTH) {
            return;
        }
        int above = screenHeight - BASE_HEIGHT - Math.min(y1, y2);
        if (above < -GRID_TOLERANCE) {
            return;
        }
        int row = (above + GRID_TOLERANCE) / ROW_HEIGHT;
        if (row >= MAX_ROWS || above - row * ROW_HEIGHT > GRID_TOLERANCE) {
            return;
        }
        liveMask |= 1 << row;
    }

    public static int rightHeight() {
        int mask = liveMask | settledMask;
        int rows = 0;
        while (rows < MAX_ROWS && (mask & (1 << rows)) != 0) {
            rows++;
        }
        if (rows == 0) {
            rows = vanillaRows();
        }
        return BASE_HEIGHT + rows * ROW_HEIGHT + reserved;
    }

    public static void reserveRight(int pixels) {
        reserved += pixels;
    }

    private static int vanillaRows() {
        Minecraft mc = Minecraft.getInstance();
        Gui gui = mc.gui;
        if (gui == null) {
            return 0;
        }
        GuiStatusHelperAccessor helper = (GuiStatusHelperAccessor) gui;
        Player player = helper.apoli$cameraPlayer();
        if (player == null) {
            return 0;
        }
        int hearts = helper.apoli$vehicleMaxHearts(helper.apoli$vehicleWithHealth());
        boolean vulnerable = mc.gameMode != null && mc.gameMode.canHurtPlayer();
        int rows = hearts > 0 ? helper.apoli$vehicleHeartRows(hearts) : (vulnerable ? 1 : 0);
        if (vulnerable && (player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply())) {
            rows++;
        }
        return rows;
    }
}
