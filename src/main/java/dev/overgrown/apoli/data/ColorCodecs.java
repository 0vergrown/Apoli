package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Function;

public final class ColorCodecs {

    private ColorCodecs() {}

    private static final Codec<Integer> HEX = Codec.STRING.comapFlatMap(ColorCodecs::parse, ColorCodecs::toHex);

    private static final Codec<Integer> CHANNELS = Codec.FLOAT.listOf().comapFlatMap(
        ColorCodecs::fromChannels, ColorCodecs::toChannels);

    public static final Codec<Integer> ARGB =
        Codec.either(HEX, Codec.either(CHANNELS, Codec.INT)).xmap(
            either -> either.map(Function.identity(), inner -> inner.map(Function.identity(), Function.identity())),
            Either::left);

    public static float red(int argb) {
        return (argb >> 16 & 0xFF) / 255.0F;
    }

    public static float green(int argb) {
        return (argb >> 8 & 0xFF) / 255.0F;
    }

    public static float blue(int argb) {
        return (argb & 0xFF) / 255.0F;
    }

    public static float alpha(int argb) {
        return (argb >>> 24 & 0xFF) / 255.0F;
    }

    public static int pack(float red, float green, float blue, float alpha) {
        return channel(alpha) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue);
    }

    public static int offsetRgb(int argb, float dRed, float dGreen, float dBlue) {
        return pack(red(argb) + dRed, green(argb) + dGreen, blue(argb) + dBlue, alpha(argb));
    }

    public static int rotateHue(int argb, float degrees) {
        float red = red(argb);
        float green = green(argb);
        float blue = blue(argb);
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float chroma = max - min;
        if (chroma <= 1.0e-5F) return argb;

        float hue;
        if (max == red) hue = ((green - blue) / chroma + 6.0F) % 6.0F;
        else if (max == green) hue = (blue - red) / chroma + 2.0F;
        else hue = (red - green) / chroma + 4.0F;
        hue = (hue * 60.0F + degrees) % 360.0F;
        if (hue < 0.0F) hue += 360.0F;

        float sector = hue / 60.0F;
        int index = (int) sector;
        float fraction = sector - index;
        float p = min;
        float q = max - chroma * fraction;
        float t = min + chroma * fraction;
        return switch (index % 6) {
            case 0 -> pack(max, t, p, alpha(argb));
            case 1 -> pack(q, max, p, alpha(argb));
            case 2 -> pack(p, max, t, alpha(argb));
            case 3 -> pack(p, q, max, alpha(argb));
            case 4 -> pack(t, p, max, alpha(argb));
            default -> pack(max, p, q, alpha(argb));
        };
    }

    private static DataResult<Integer> parse(String raw) {
        boolean packed = raw.startsWith("0x") || raw.startsWith("0X");
        String digits = raw.startsWith("#") ? raw.substring(1) : packed ? raw.substring(2) : raw;
        if (digits.length() != 6 && digits.length() != 8) {
            return DataResult.error(() -> "'" + raw + "' is not a colour — expected #RRGGBB, #RRGGBBAA or 0xAARRGGBB");
        }
        try {
            long value = Long.parseLong(digits, 16);
            if (digits.length() == 6) return DataResult.success((int) (value | 0xFF000000L));
            int eight = (int) value;
            return DataResult.success(packed ? eight : rgbaToArgb(eight));
        } catch (NumberFormatException ignored) {
            return DataResult.error(() -> "'" + raw + "' is not a hexadecimal colour");
        }
    }

    private static int rgbaToArgb(int rgba) {
        return (rgba >>> 8) | (rgba << 24);
    }

    private static int argbToRgba(int argb) {
        return (argb << 8) | (argb >>> 24);
    }

    private static String toHex(int argb) {
        return String.format("#%08X", argbToRgba(argb));
    }

    private static DataResult<Integer> fromChannels(List<Float> channels) {
        if (channels.size() != 3 && channels.size() != 4) {
            return DataResult.error(() -> "A colour list needs 3 (rgb) or 4 (rgba) values");
        }
        int alpha = channels.size() == 4 ? channel(channels.get(3)) : 255;
        return DataResult.success(alpha << 24 | channel(channels.get(0)) << 16
            | channel(channels.get(1)) << 8 | channel(channels.get(2)));
    }

    private static List<Float> toChannels(int argb) {
        return List.of(red(argb), green(argb), blue(argb), alpha(argb));
    }

    private static int channel(float value) {
        return Mth.clamp(Math.round(value * 255.0F), 0, 255);
    }
}
