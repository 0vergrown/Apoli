package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class PreventDeathPower extends PowerType<PreventDeathPower.Config> {
    public record Config(Optional<DamageCondition> damageCondition, Optional<EntityAction> entityAction) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            DamageCondition.CODEC.optionalFieldOf("damage_condition").forGetter(Config::damageCondition),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction)
        ).apply(i, Config::new));
    }
}
