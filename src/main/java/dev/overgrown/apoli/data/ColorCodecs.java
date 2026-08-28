package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

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

    private static DataResult<Integer> parse(String raw) {
        String digits = raw.startsWith("#") ? raw.substring(1)
            : (raw.startsWith("0x") || raw.startsWith("0X")) ? raw.substring(2) : raw;
        if (digits.length() != 6 && digits.length() != 8) {
            return DataResult.error(() -> "'" + raw + "' is not a colour — expected #RRGGBB or #AARRGGBB");
        }
        try {
            long value = Long.parseLong(digits, 16);
            return DataResult.success((int) (digits.length() == 6 ? value | 0xFF000000L : value));
        } catch (NumberFormatException ignored) {
            return DataResult.error(() -> "'" + raw + "' is not a hexadecimal colour");
        }
    }

    private static String toHex(int argb) {
        return String.format("#%08X", argb);
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
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }
}
