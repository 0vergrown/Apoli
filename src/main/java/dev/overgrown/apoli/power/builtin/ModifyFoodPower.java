package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModifyFoodPower extends PowerType<ModifyFoodPower.Config> {

    public record Config(
        Optional<EntityAction> entityAction,
        Optional<ItemAction> itemAction,
        Optional<ItemCondition> itemCondition,
        Optional<List<AttributeModifier>> foodModifiers,
        Optional<List<AttributeModifier>> saturationModifiers,
        Optional<List<AttributeModifier>> eatTicksModifiers,
        Optional<ItemStackData> replaceStack,
        boolean preventEffects,
        boolean alwaysEdible
    ) {}

    private static final MapCodec<Config> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
        dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("item_action", ItemAction.CODEC).forGetter(Config::itemAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
        AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("food_modifiers").forGetter(Config::foodModifiers),
        AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("saturation_modifiers").forGetter(Config::saturationModifiers),
        AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("eat_ticks_modifiers").forGetter(Config::eatTicksModifiers),
        ItemStackData.CODEC.optionalFieldOf("replace_stack").forGetter(Config::replaceStack),
        Codec.BOOL.optionalFieldOf("prevent_effects", false).forGetter(Config::preventEffects),
        Codec.BOOL.optionalFieldOf("always_edible", false).forGetter(Config::alwaysEdible)
    ).apply(i, Config::new));

    private static final MapCodec<Config> ALIASED = AliasingMapCodec.wrap(INNER, Map.of(
        "food_modifier", "food_modifiers",
        "saturation_modifier", "saturation_modifiers",
        "eat_ticks_modifier", "eat_ticks_modifiers",
        "can_always_eat", "always_edible"
    ));

    @Override
    public MapCodec<Config> configCodec() {
        return ALIASED;
    }
}
