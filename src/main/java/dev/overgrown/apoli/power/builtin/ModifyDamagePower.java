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
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::bientityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("self_action", EntityAction.CODEC).forGetter(Config::selfAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("target_action", EntityAction.CODEC).forGetter(Config::targetAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("attacker_action", EntityAction.CODEC).forGetter(Config::attackerAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("target_condition", EntityCondition.CODEC).forGetter(Config::targetCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("apply_armor_condition", EntityCondition.CODEC).forGetter(Config::applyArmorCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("damage_armor_condition", EntityCondition.CODEC).forGetter(Config::damageArmorCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("damage_condition", DamageCondition.CODEC).forGetter(Config::damageCondition),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            Codec.BOOL.optionalFieldOf("target_used", false).forGetter(Config::targetUsed)
        ).apply(i, Config::new));
    }
}
