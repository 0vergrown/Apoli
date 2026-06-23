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
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            BlockAction.CODEC.optionalFieldOf("block_action").forGetter(Config::blockAction),
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition),
            Codec.BOOL.optionalFieldOf("only_when_harvested", true).forGetter(Config::onlyWhenHarvested)
        ).apply(i, Config::new));
    }
}
