package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public class PreventBeeAngerPower extends PowerType<PreventBeeAngerPower.Config> {
    public record Config(
            Optional<BlockCondition> blockCondition,
            Optional<BiEntityCondition> biEntityCondition,
            Optional<BiEntityAction> biEntityAction
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec( i -> i.group (
                LoggedOptionalField.of("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
                LoggedOptionalField.of("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::biEntityCondition),
                LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::biEntityAction)
        ).apply(i, Config::new));
    }
}
