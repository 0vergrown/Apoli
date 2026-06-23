package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;

import java.util.List;
import java.util.Optional;

public final class PreventFeatureRenderPower extends PowerType<PreventFeatureRenderPower.Config> {
    public record Config(Optional<String> feature, Optional<List<String>> features) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("feature").forGetter(Config::feature),
            Codec.STRING.listOf().optionalFieldOf("features").forGetter(Config::features)
        ).apply(i, Config::new));
    }
}
