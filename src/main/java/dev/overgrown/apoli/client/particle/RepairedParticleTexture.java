package dev.overgrown.apoli.client.particle;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overgrown.apoli.Apoli;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public final class RepairedParticleTexture extends SimpleTexture {

    private static final int OPAQUE = 0xFF;
    private static final int TOLERANCE = 12;
    private static final float SUSPECT_SHARE = 0.15F;
    private static final int MAX_ANALYSED = 512 * 512;

    private final boolean bleed;

    public RepairedParticleTexture(ResourceLocation source, boolean bleed) {
        super(source);
        this.bleed = bleed;
    }

    @Override
    protected TextureImage getTextureImage(ResourceManager resourceManager) {
        TextureImage loaded = super.getTextureImage(resourceManager);
        NativeImage image;
        try {
            image = loaded.getImage();
        } catch (IOException ignored) {
            return loaded;
        }
        if (repair(image, this.bleed) && !this.bleed) {
            Apoli.LOGGER.warn("[Apoli] Particle texture {} loses its colour across the semi-transparent pixels "
                    + "around its edge, so those pixels draw as a dark fringe that grows as the particle fills more "
                    + "of the screen. Either fill the colour in under the transparency in the PNG, or set "
                    + "\"alpha_bleed\": true on the apoli:custom particle to have Apoli fill it in at load.",
                this.location);
        }
        return loaded;
    }

    static boolean repair(NativeImage image, boolean apply) {
        int width = image.getWidth();
        int height = image.getHeight();
        int size = width * height;
        if (size <= 0 || image.format() != NativeImage.Format.RGBA) return false;
        if (!scan(image, width, height)) return false;
        if (!apply && size > MAX_ANALYSED) return false;

        int[] original = new int[size];
        int[] dilated = new int[size];
        int[] alpha = new int[size];
        int[] queue = new int[size];
        boolean[] filled = new boolean[size];
        int tail = 0;
        int semiTransparent = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int packed = image.getPixelRGBA(x, y);
                int a = (packed >>> 24) & 0xFF;
                alpha[index] = a;
                original[index] = packed & 0x00FFFFFF;
                dilated[index] = original[index];
                if (a == OPAQUE) {
                    filled[index] = true;
                    queue[tail++] = index;
                } else if (a != 0) {
                    semiTransparent++;
                }
            }
        }
        if (semiTransparent == 0 || tail == 0) return false;

        int head = 0;
        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            int y = index / width;
            for (int dy = -1; dy <= 1; dy++) {
                int ny = y + dy;
                if (ny < 0 || ny >= height) continue;
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    if (nx < 0 || nx >= width || (dx == 0 && dy == 0)) continue;
                    int neighbour = ny * width + nx;
                    if (filled[neighbour]) continue;
                    filled[neighbour] = true;
                    dilated[neighbour] = average(dilated, filled, width, height, nx, ny);
                    queue[tail++] = neighbour;
                }
            }
        }

        int suspect = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int a = alpha[index];
                if (a == OPAQUE) continue;
                int source = original[index];
                int target = dilated[index];
                if (a != 0 && darkerBy(source, target) > TOLERANCE) suspect++;
                if (apply) image.setPixelRGBA(x, y, (a << 24) | target);
            }
        }
        return suspect > semiTransparent * SUSPECT_SHARE;
    }

    private static boolean scan(NativeImage image, int width, int height) {
        boolean opaque = false;
        boolean semiTransparent = false;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int a = (image.getPixelRGBA(x, y) >>> 24) & 0xFF;
                if (a == OPAQUE) opaque = true;
                else if (a != 0) semiTransparent = true;
                if (opaque && semiTransparent) return true;
            }
        }
        return false;
    }

    private static int darkerBy(int source, int target) {
        int red = (target & 0xFF) - (source & 0xFF);
        int green = ((target >>> 8) & 0xFF) - ((source >>> 8) & 0xFF);
        int blue = ((target >>> 16) & 0xFF) - ((source >>> 16) & 0xFF);
        return Math.max(red, Math.max(green, blue));
    }

    private static int average(int[] colour, boolean[] filled, int width, int height, int x, int y) {
        int red = 0;
        int green = 0;
        int blue = 0;
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            int ny = y + dy;
            if (ny < 0 || ny >= height) continue;
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                if (nx < 0 || nx >= width || (dx == 0 && dy == 0)) continue;
                int index = ny * width + nx;
                if (!filled[index]) continue;
                int packed = colour[index];
                red += packed & 0xFF;
                green += (packed >>> 8) & 0xFF;
                blue += (packed >>> 16) & 0xFF;
                count++;
            }
        }
        if (count == 0) return 0;
        return ((blue / count) << 16) | ((green / count) << 8) | (red / count);
    }
}
