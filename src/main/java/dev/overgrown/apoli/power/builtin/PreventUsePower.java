package dev.overgrown.apoli.power.builtin;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.Hand;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.PowerType;

import java.util.List;
import java.util.Optional;

public final class PreventUsePower extends PowerType<PreventUsePower.Config> {
    public record Config(
        Optional<BiEntityAction> bientityAction,
        Optional<ItemAction> heldItemAction,
        Optional<ItemAction> resultItemAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<ItemCondition> itemCondition,
        List<Hand> hands,
        Optional<ItemStackData> resultStack,
        boolean targetUsed
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Config::bientityAction),
            ItemAction.CODEC.optionalFieldOf("held_item_action").forGetter(Config::heldItemAction),
            ItemAction.CODEC.optionalFieldOf("result_item_action").forGetter(Config::resultItemAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition),
            Hand.LIST_CODEC.optionalFieldOf("hands", Hand.BOTH).forGetter(Config::hands),
            ItemStackData.CODEC.optionalFieldOf("result_stack").forGetter(Config::resultStack),
            Codec.BOOL.optionalFieldOf("target_used", false).forGetter(Config::targetUsed)
        ).apply(i, Config::new));
    }

    public static JsonObject targetUsed(boolean value) {
        JsonObject obj = new JsonObject();
        obj.add("target_used", new JsonPrimitive(value));
        return obj;
    }
}
