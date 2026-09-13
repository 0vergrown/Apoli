package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class ModifyFogPower extends PowerType<ModifyFogPower.Config> {
    public record Config(
            Optional<Float> s,
            Optional<Float> v,

            Optional<Float> r,
            Optional<Float> g,
            Optional<Float> b,

            float fade_in,
            float fade_out,

            int priority
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.optionalFieldOf("s").forGetter(Config::s),
                Codec.FLOAT.optionalFieldOf("v").forGetter(Config::v),

                Codec.FLOAT.optionalFieldOf("r").forGetter(Config::r),
                Codec.FLOAT.optionalFieldOf("g").forGetter(Config::g),
                Codec.FLOAT.optionalFieldOf("b").forGetter(Config::b),

                Codec.FLOAT.optionalFieldOf("fade_in", 0f).forGetter(Config::fade_in),
                Codec.FLOAT.optionalFieldOf("fade_out", 0f).forGetter(Config::fade_out),

                Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }
}
