package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;

public final class ModifyFallingPower extends PowerType<ModifyFallingPower.Config> {
    public record Config(float velocity, boolean takeFallDamage) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("velocity").forGetter(Config::velocity),
            Codec.BOOL.optionalFieldOf("take_fall_damage", true).forGetter(Config::takeFallDamage)
        ).apply(i, Config::new));
    }
}
