package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class PreventEntityCollisionPower extends PowerType<PreventEntityCollisionPower.Config> {
    public record Config(Optional<BiEntityCondition> bientityCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }
}
