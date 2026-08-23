package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class ActionOnItemPickupPower extends PowerType<ActionOnItemPickupPower.Config> {
    public record Config(
        Optional<BiEntityAction> bientityAction,
        Optional<ItemAction> itemAction,
        Optional<BiEntityCondition> bientityCondition,
        Optional<ItemCondition> itemCondition
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::bientityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("item_action", ItemAction.CODEC).forGetter(Config::itemAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition)
        ).apply(i, Config::new));
    }
}
