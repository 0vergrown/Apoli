package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.PowerType;

import java.util.List;
import java.util.Optional;

public final class ModifyDamagePower extends PowerType<ModifyDamagePower.Config> {
    public record Config(
        Optional<BiEntityAction> bientityAction,
        Optional<EntityAction> selfAction,
        Optional<EntityAction> targetAction,
        Optional<EntityAction> attackerAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<EntityCondition> targetCondition,
        Optional<EntityCondition> applyArmorCondition,
        Optional<EntityCondition> damageArmorCondition,
        Optional<DamageCondition> damageCondition,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers,
        boolean targetUsed
    ) {
        public List<AttributeModifier> allModifiers() {
            if (modifier.isEmpty() && modifiers.isEmpty()) return List.of();
            if (modifier.isPresent() && modifiers.isEmpty()) return List.of(modifier.get());
            if (modifier.isEmpty()) return modifiers.get();
            List<AttributeModifier> combined = new java.util.ArrayList<>(modifiers.get().size() + 1);
            combined.add(modifier.get());
            combined.addAll(modifiers.get());
            return combined;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Config::bientityAction),
            EntityAction.CODEC.optionalFieldOf("self_action").forGetter(Config::selfAction),
            EntityAction.CODEC.optionalFieldOf("target_action").forGetter(Config::targetAction),
            EntityAction.CODEC.optionalFieldOf("attacker_action").forGetter(Config::attackerAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            EntityCondition.CODEC.optionalFieldOf("target_condition").forGetter(Config::targetCondition),
            EntityCondition.CODEC.optionalFieldOf("apply_armor_condition").forGetter(Config::applyArmorCondition),
            EntityCondition.CODEC.optionalFieldOf("damage_armor_condition").forGetter(Config::damageArmorCondition),
            DamageCondition.CODEC.optionalFieldOf("damage_condition").forGetter(Config::damageCondition),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            Codec.BOOL.optionalFieldOf("target_used", false).forGetter(Config::targetUsed)
        ).apply(i, Config::new));
    }
}
