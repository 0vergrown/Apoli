package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.ActionResult;
import dev.overgrown.apoli.data.Hand;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;

import java.util.List;
import java.util.Optional;

public final class ActionOnBlockUsePower extends PowerType<ActionOnBlockUsePower.Config> {
    public record Config(
        Optional<EntityAction> entityAction,
        Optional<BlockAction> blockAction,
        Optional<BlockCondition> blockCondition,
        Optional<ItemCondition> itemCondition,
        List<PreventBlockPlacePower.Dir> directions,
        List<Hand> hands,
        Optional<ItemStackData> resultStack,
        Optional<ItemAction> heldItemAction,
        Optional<ItemAction> resultItemAction,
        ActionResult actionResult
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            BlockAction.CODEC.optionalFieldOf("block_action").forGetter(Config::blockAction),
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition),
            PreventBlockPlacePower.Dir.LIST_CODEC.optionalFieldOf("directions", PreventBlockPlacePower.Dir.ALL).forGetter(Config::directions),
            Hand.LIST_CODEC.optionalFieldOf("hands", Hand.BOTH).forGetter(Config::hands),
            ItemStackData.CODEC.optionalFieldOf("result_stack").forGetter(Config::resultStack),
            ItemAction.CODEC.optionalFieldOf("held_item_action").forGetter(Config::heldItemAction),
            ItemAction.CODEC.optionalFieldOf("result_item_action").forGetter(Config::resultItemAction),
            ActionResult.CODEC.optionalFieldOf("action_result", ActionResult.SUCCESS).forGetter(Config::actionResult)
        ).apply(i, Config::new));
    }
}
