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
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("block_action", BlockAction.CODEC).forGetter(Config::blockAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            PreventBlockPlacePower.Dir.LIST_CODEC.optionalFieldOf("directions", PreventBlockPlacePower.Dir.ALL).forGetter(Config::directions),
            Hand.LIST_CODEC.optionalFieldOf("hands", Hand.BOTH).forGetter(Config::hands),
            ItemStackData.CODEC.optionalFieldOf("result_stack").forGetter(Config::resultStack),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("held_item_action", ItemAction.CODEC).forGetter(Config::heldItemAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("result_item_action", ItemAction.CODEC).forGetter(Config::resultItemAction),
            ActionResult.CODEC.optionalFieldOf("action_result", ActionResult.SUCCESS).forGetter(Config::actionResult)
        ).apply(i, Config::new));
    }
}
