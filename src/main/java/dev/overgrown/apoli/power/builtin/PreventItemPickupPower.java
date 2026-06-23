package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class PreventItemPickupPower extends PowerType<PreventItemPickupPower.Config> {
    public record Config(
        Optional<BiEntityAction> bientityActionThrower,
        Optional<BiEntityAction> bientityActionItem,
        Optional<ItemAction> itemAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<ItemCondition> itemCondition,
        int priority
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.optionalFieldOf("bientity_action_thrower").forGetter(Config::bientityActionThrower),
            BiEntityAction.CODEC.optionalFieldOf("bientity_action_item").forGetter(Config::bientityActionItem),
            ItemAction.CODEC.optionalFieldOf("item_action").forGetter(Config::itemAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Config::bientityCondition),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }
}
