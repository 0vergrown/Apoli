package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.PowerType;

import java.util.List;
import java.util.Optional;

public final class ModifyBreakSpeedPower extends PowerType<ModifyBreakSpeedPower.Config> {
    public record Config(
        Optional<BlockCondition> blockCondition,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers,
        Optional<AttributeModifier> hardnessModifier,
        Optional<List<AttributeModifier>> hardnessModifiers
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            AttributeModifier.CODEC.optionalFieldOf("hardness_modifier").forGetter(Config::hardnessModifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("hardness_modifiers").forGetter(Config::hardnessModifiers)
        ).apply(i, Config::new));
    }
}
