package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class ClimbingPower extends PowerType<ClimbingPower.Config> {
    public record Config(boolean allowHolding, Optional<EntityCondition> holdCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("allow_holding", true).forGetter(Config::allowHolding),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("hold_condition", EntityCondition.CODEC).forGetter(Config::holdCondition)
        ).apply(i, Config::new));
    }
}
