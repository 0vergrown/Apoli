package dev.overgrown.apoli.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.IconData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class IconRenderer {

    private static final int FALLBACK_TEXTURE_SIZE = 16;
    private static final Map<ResourceLocation, int[]> TEXTURE_SIZES = new HashMap<>();

    private IconRenderer() {}

    public static void render(GuiGraphics graphics, IconData icon, int x, int y) {
        render(graphics, icon, x, y, 16, 16, false);
    }

    public static void renderFake(GuiGraphics graphics, IconData icon, int x, int y) {
        render(graphics, icon, x, y, 16, 16, true);
    }

    public static void render(GuiGraphics graphics, IconData icon, int x, int y, int width, int height, boolean fake) {
        ResourceLocation texture = icon.texture().orElse(null);
        if (texture != null) {
            renderTexture(graphics, texture, icon.width(), icon.height(), x, y, width, height);
            return;
        }
        if (icon.stack().isEmpty()) return;
        if (fake) {
            graphics.renderFakeItem(icon.stack(), x, y);
        } else {
            graphics.renderItem(icon.stack(), x, y);
        }
    }

    private static void renderTexture(GuiGraphics graphics, ResourceLocation texture, int declaredWidth,
                                      int declaredHeight, int x, int y, int width, int height) {
        int[] size = sizeOf(texture);
        int textureWidth = size[0];
        int textureHeight = size[1];
        int regionWidth = declaredWidth > 0 ? Math.min(declaredWidth, textureWidth) : textureWidth;
        int regionHeight = declaredHeight > 0 ? Math.min(declaredHeight, textureHeight) : textureHeight;

        AbstractTexture bound = Minecraft.getInstance().getTextureManager().getTexture(texture);
        bound.setFilter(regionWidth > width || regionHeight > height, false);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(texture, x, y, width, height, 0.0F, 0.0F, regionWidth, regionHeight,
            textureWidth, textureHeight);
    }

    private static int[] sizeOf(ResourceLocation texture) {
        int[] cached = TEXTURE_SIZES.get(texture);
        if (cached != null) return cached;
        int[] size = {FALLBACK_TEXTURE_SIZE, FALLBACK_TEXTURE_SIZE};
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(texture);
             NativeImage image = NativeImage.read(in)) {
            size = new int[]{Math.max(1, image.getWidth()), Math.max(1, image.getHeight())};
        } catch (Exception e) {
            Apoli.LOGGER.warn("[Apoli] Could not read icon texture {}, assuming {}x{}: {}",
                texture, FALLBACK_TEXTURE_SIZE, FALLBACK_TEXTURE_SIZE, e.toString());
        }
        TEXTURE_SIZES.put(texture, size);
        return size;
    }

    public static void clearCache() {
        TEXTURE_SIZES.clear();
    }
}
