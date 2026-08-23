package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.Hand;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;

import java.util.List;
import java.util.Optional;

public final class ActionOnBlockPlacePower extends PowerType<ActionOnBlockPlacePower.Config> {
    public record Config(
        Optional<EntityAction> entityAction,
        Optional<ItemAction> heldItemAction,
        Optional<BlockAction> placeToAction,
        Optional<BlockAction> placeOnAction,
        Optional<ItemCondition> itemCondition,
        Optional<BlockCondition> placeToCondition,
        Optional<BlockCondition> placeOnCondition,
        List<PreventBlockPlacePower.Dir> directions,
        List<Hand> hands,
        Optional<ItemStackData> resultStack,
        Optional<ItemAction> resultItemAction
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("held_item_action", ItemAction.CODEC).forGetter(Config::heldItemAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("place_to_action", BlockAction.CODEC).forGetter(Config::placeToAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("place_on_action", BlockAction.CODEC).forGetter(Config::placeOnAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("place_to_condition", BlockCondition.CODEC).forGetter(Config::placeToCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("place_on_condition", BlockCondition.CODEC).forGetter(Config::placeOnCondition),
            PreventBlockPlacePower.Dir.LIST_CODEC.optionalFieldOf("directions", PreventBlockPlacePower.Dir.ALL).forGetter(Config::directions),
            Hand.LIST_CODEC.optionalFieldOf("hands", Hand.BOTH).forGetter(Config::hands),
            ItemStackData.CODEC.optionalFieldOf("result_stack").forGetter(Config::resultStack),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("result_item_action", ItemAction.CODEC).forGetter(Config::resultItemAction)
        ).apply(i, Config::new));
    }
}
