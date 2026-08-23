package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class ActionOnBlockBreakPower extends PowerType<ActionOnBlockBreakPower.Config> {
    public record Config(
        Optional<EntityAction> entityAction,
        Optional<BlockAction> blockAction,
        Optional<BlockCondition> blockCondition,
        boolean onlyWhenHarvested
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("block_action", BlockAction.CODEC).forGetter(Config::blockAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
            Codec.BOOL.optionalFieldOf("only_when_harvested", true).forGetter(Config::onlyWhenHarvested)
        ).apply(i, Config::new));
    }
}
