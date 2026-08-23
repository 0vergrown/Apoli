package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class EntityGlowPower extends PowerType<EntityGlowPower.Config> {
    public record Config(
        Optional<EntityCondition> entityCondition,
        Optional<BiEntityCondition> bientityCondition,
        boolean useTeams,
        float red,
        float green,
        float blue,
        boolean selfGlowTarget
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("entity_condition", EntityCondition.CODEC).forGetter(Config::entityCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            Codec.BOOL.optionalFieldOf("use_teams", true).forGetter(Config::useTeams),
            Codec.FLOAT.optionalFieldOf("red", 1.0f).forGetter(Config::red),
            Codec.FLOAT.optionalFieldOf("green", 1.0f).forGetter(Config::green),
            Codec.FLOAT.optionalFieldOf("blue", 1.0f).forGetter(Config::blue),
            Codec.BOOL.optionalFieldOf("self_glow_target", false).forGetter(Config::selfGlowTarget)
        ).apply(i, Config::new));
    }
}
