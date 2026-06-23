package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class InvisibilityPower extends PowerType<InvisibilityPower.Config> {
    public record Config(
        boolean renderArmor,
        boolean renderOutline,
        Optional<BiEntityCondition> bientityCondition
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("render_armor", false).forGetter(Config::renderArmor),
            Codec.BOOL.optionalFieldOf("render_outline", false).forGetter(Config::renderOutline),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }
}
