package dev.overgrown.apoli.client.model;

import com.mojang.blaze3d.platform.NativeImage;

public final class TextureMask {
    public static final TextureMask NONE = new TextureMask(0, 0, new byte[0]);

    private static final int SOLID = 140;

    private final int width;
    private final int height;
    private final byte[] alpha;

    private TextureMask(int width, int height, byte[] alpha) {
        this.width = width;
        this.height = height;
        this.alpha = alpha;
    }

    public static TextureMask of(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] alpha = new byte[width * height];
        if (image.format() != NativeImage.Format.RGBA) {
            java.util.Arrays.fill(alpha, (byte) 0xFF);
            return new TextureMask(width, height, alpha);
        }
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                alpha[row + x] = (byte) (image.getPixelRGBA(x, y) >>> 24);
            }
        }
        return new TextureMask(width, height, alpha);
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public boolean isEmpty() {
        return this.width == 0 || this.height == 0;
    }

    public boolean visible(int x, int y) {
        return x >= 0 && y >= 0 && x < this.width && y < this.height && this.alpha[y * this.width + x] != 0;
    }

    public boolean solid(int x, int y) {
        return x >= 0 && y >= 0 && x < this.width && y < this.height
            && (this.alpha[y * this.width + x] & 0xFF) > SOLID;
    }
}
