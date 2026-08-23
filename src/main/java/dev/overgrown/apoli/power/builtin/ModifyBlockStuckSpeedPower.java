package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.data.Vector;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class ModifyBlockStuckSpeedPower extends PowerType<ModifyBlockStuckSpeedPower.Config> {
    public record Config(Optional<BlockCondition> blockCondition, Vector multiplier) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
            Vector.SCALAR_OR_VECTOR.optionalFieldOf("multiplier", Vector.uniform(1.0f)).forGetter(Config::multiplier)
        ).apply(i, Config::new));
    }
}
