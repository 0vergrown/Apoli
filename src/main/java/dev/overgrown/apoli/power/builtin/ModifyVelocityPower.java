package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.builtin.bientity.RelativeRotationCondition;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.PowerType;

import java.util.List;
import java.util.Optional;

public final class ModifyVelocityPower extends PowerType<ModifyVelocityPower.Config> {
    public record Config(
        List<RelativeRotationCondition.Axis> axes,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers
    ) {}

    private static final List<RelativeRotationCondition.Axis> ALL_AXES = List.of(
        RelativeRotationCondition.Axis.X,
        RelativeRotationCondition.Axis.Y,
        RelativeRotationCondition.Axis.Z
    );

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            RelativeRotationCondition.Axis.LIST_CODEC.optionalFieldOf("axes", ALL_AXES).forGetter(Config::axes),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }
}
