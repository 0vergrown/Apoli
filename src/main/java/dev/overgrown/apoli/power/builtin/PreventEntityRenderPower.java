package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class PreventEntityRenderPower extends PowerType<PreventEntityRenderPower.Config> {
    public record Config(
        Optional<EntityCondition> entityCondition,
        Optional<BiEntityCondition> bientityCondition
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("entity_condition", EntityCondition.CODEC).forGetter(Config::entityCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }
}
