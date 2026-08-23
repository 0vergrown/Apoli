package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class ModifyHarvestPower extends PowerType<ModifyHarvestPower.Config> {
    public record Config(Optional<BlockCondition> blockCondition, boolean allow) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
            Codec.BOOL.fieldOf("allow").forGetter(Config::allow)
        ).apply(i, Config::new));
    }
}
