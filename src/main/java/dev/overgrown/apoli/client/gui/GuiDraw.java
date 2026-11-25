package dev.overgrown.apoli.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

@Environment(EnvType.CLIENT)
public final class GuiDraw {
    private static final ResourceLocation SLIDER = new ResourceLocation("textures/gui/slider.png");
    private static final ResourceLocation PANEL = new ResourceLocation("textures/gui/demo_background.png");
    private static final ResourceLocation CHEST = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final int ATLAS = 256;
    private static final int PANEL_WIDTH = 248;
    private static final int PANEL_HEIGHT = 166;
    private static final int PANEL_BORDER = 4;

    private GuiDraw() {}

    public static void button(GuiGraphics graphics, int x, int y, int width, int height, boolean active, boolean hovered) {
        int row = !active ? 0 : hovered ? 2 : 1;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, x, y, width, height, 20, 4, 200, 20, 0, 46 + row * 20);
        RenderSystem.disableBlend();
    }

    public static void slider(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blitNineSliced(SLIDER, x, y, width, height, 20, 4, 200, 20, 0, 0);
        RenderSystem.disableBlend();
    }

    public static void handle(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blitNineSliced(SLIDER, x, y, width, height, 20, 4, 200, 20, 0, hovered ? 60 : 40);
        RenderSystem.disableBlend();
    }

    public static void texture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        region(graphics, texture, x, y, width, height, 0, 0, width, height, width, height);
    }

    public static void region(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                              int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        if (width <= 0 || height <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        RenderSystem.disableBlend();
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        int b = PANEL_BORDER;
        int innerWidth = Math.max(0, width - b * 2);
        int innerHeight = Math.max(0, height - b * 2);
        int sourceWidth = PANEL_WIDTH - b * 2;
        int sourceHeight = PANEL_HEIGHT - b * 2;
        int right = PANEL_WIDTH - b;
        int bottom = PANEL_HEIGHT - b;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(PANEL, x, y, b, b, 0, 0, b, b, ATLAS, ATLAS);
        graphics.blit(PANEL, x + b + innerWidth, y, b, b, right, 0, b, b, ATLAS, ATLAS);
        graphics.blit(PANEL, x, y + b + innerHeight, b, b, 0, bottom, b, b, ATLAS, ATLAS);
        graphics.blit(PANEL, x + b + innerWidth, y + b + innerHeight, b, b, right, bottom, b, b, ATLAS, ATLAS);
        if (innerWidth > 0) {
            graphics.blit(PANEL, x + b, y, innerWidth, b, b, 0, sourceWidth, b, ATLAS, ATLAS);
            graphics.blit(PANEL, x + b, y + b + innerHeight, innerWidth, b, b, bottom, sourceWidth, b, ATLAS, ATLAS);
        }
        if (innerHeight > 0) {
            graphics.blit(PANEL, x, y + b, b, innerHeight, 0, b, b, sourceHeight, ATLAS, ATLAS);
            graphics.blit(PANEL, x + b + innerWidth, y + b, b, innerHeight, right, b, b, sourceHeight, ATLAS, ATLAS);
        }
        if (innerWidth > 0 && innerHeight > 0) {
            graphics.blit(PANEL, x + b, y + b, innerWidth, innerHeight, b, b, sourceWidth, sourceHeight, ATLAS, ATLAS);
        }
        RenderSystem.disableBlend();
    }

    public static void slotFrame(GuiGraphics graphics, int x, int y) {
        graphics.blit(CHEST, x, y, 7, 17, 18, 18);
    }

    public static void entity(GuiGraphics graphics, int x, int y, int width, int height, int scale, boolean followMouse,
                              int mouseX, int mouseY, LivingEntity entity) {
        int feetX = x + width / 2;
        int feetY = y + height / 2 + Math.round(entity.getBbHeight() * scale / 2.0F);
        float lookX = followMouse ? feetX - mouseX : 0.0F;
        float lookY = followMouse ? feetY - entity.getEyeHeight() * scale - mouseY : 0.0F;
        graphics.enableScissor(x, y, x + width, y + height);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, feetX, feetY, scale, lookX, lookY, entity);
        graphics.disableScissor();
    }
}
