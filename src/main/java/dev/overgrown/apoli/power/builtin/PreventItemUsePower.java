package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class PreventItemUsePower extends PowerType<PreventItemUsePower.Config> {
    public record Config(Optional<ItemCondition> itemCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition)
        ).apply(i, Config::new));
    }
}
