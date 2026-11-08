package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Map;

public record BlockGlowStyle(float red, float green, float blue, boolean respectBounding, Mode mode) {

    public static final int FLAG_RESPECT_BOUNDING = 1;
    public static final int FLAG_XRAY = 2;

    public enum Mode implements StringRepresentable {
        OUTLINE("outline"),
        GLOW("glow");

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<BlockGlowStyle> MAP_CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("red", 1.0F).forGetter(BlockGlowStyle::red),
            Codec.FLOAT.optionalFieldOf("green", 1.0F).forGetter(BlockGlowStyle::green),
            Codec.FLOAT.optionalFieldOf("blue", 1.0F).forGetter(BlockGlowStyle::blue),
            Codec.BOOL.optionalFieldOf("respect_bounding", true).forGetter(BlockGlowStyle::respectBounding),
            Mode.CODEC.optionalFieldOf("mode", Mode.OUTLINE).forGetter(BlockGlowStyle::mode)
        ).apply(i, BlockGlowStyle::new)),
        Map.of("r", "red", "g", "green", "b", "blue"));

    public int color() {
        return ColorCodecs.pack(red, green, blue, 1.0F);
    }

    public int flags() {
        return (respectBounding ? FLAG_RESPECT_BOUNDING : 0) | (mode == Mode.GLOW ? FLAG_XRAY : 0);
    }
}
