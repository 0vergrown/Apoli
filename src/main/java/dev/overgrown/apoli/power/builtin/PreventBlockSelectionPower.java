package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class PreventBlockSelectionPower extends PowerType<PreventBlockSelectionPower.Config> {
    public record Config(Optional<BlockCondition> blockCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition)
        ).apply(i, Config::new));
    }
}
