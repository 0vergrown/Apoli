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
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            ItemAction.CODEC.optionalFieldOf("held_item_action").forGetter(Config::heldItemAction),
            BlockAction.CODEC.optionalFieldOf("place_to_action").forGetter(Config::placeToAction),
            BlockAction.CODEC.optionalFieldOf("place_on_action").forGetter(Config::placeOnAction),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition),
            BlockCondition.CODEC.optionalFieldOf("place_to_condition").forGetter(Config::placeToCondition),
            BlockCondition.CODEC.optionalFieldOf("place_on_condition").forGetter(Config::placeOnCondition),
            PreventBlockPlacePower.Dir.LIST_CODEC.optionalFieldOf("directions", PreventBlockPlacePower.Dir.ALL).forGetter(Config::directions),
            Hand.LIST_CODEC.optionalFieldOf("hands", Hand.BOTH).forGetter(Config::hands),
            ItemStackData.CODEC.optionalFieldOf("result_stack").forGetter(Config::resultStack),
            ItemAction.CODEC.optionalFieldOf("result_item_action").forGetter(Config::resultItemAction)
        ).apply(i, Config::new));
    }
}
