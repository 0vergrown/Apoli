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
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action_thrower", BiEntityAction.CODEC).forGetter(Config::bientityActionThrower),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action_item", BiEntityAction.CODEC).forGetter(Config::bientityActionItem),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("item_action", ItemAction.CODEC).forGetter(Config::itemAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }
}
