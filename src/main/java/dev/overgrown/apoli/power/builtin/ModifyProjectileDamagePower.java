package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.PowerType;

import java.util.List;
import java.util.Optional;

public final class ModifyProjectileDamagePower extends PowerType<ModifyProjectileDamagePower.Config> {
    public record Config(
        Optional<DamageCondition> damageCondition,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers,
        Optional<EntityCondition> targetCondition,
        Optional<EntityAction> selfAction,
        Optional<EntityAction> targetAction
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            DamageCondition.CODEC.optionalFieldOf("damage_condition").forGetter(Config::damageCondition),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            EntityCondition.CODEC.optionalFieldOf("target_condition").forGetter(Config::targetCondition),
            EntityAction.CODEC.optionalFieldOf("self_action").forGetter(Config::selfAction),
            EntityAction.CODEC.optionalFieldOf("target_action").forGetter(Config::targetAction)
        ).apply(i, Config::new));
    }
}
