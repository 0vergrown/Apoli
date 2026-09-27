package dev.overgrown.apoli.compat.voicechat;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class ActionOnSpeakPower extends PowerType<ActionOnSpeakPower.Config> {
    public record Config(Optional<EntityAction> actionOnSpeak,
                         Optional<EntityAction> actionOnStopSpeaking,
                         Optional<BiEntityAction> bientityActionOnSpeak,
                         Optional<BiEntityAction> bientityActionOnStopSpeaking,
                         Optional<BiEntityCondition> bientityCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::actionOnSpeak),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action_stop", EntityAction.CODEC).forGetter(Config::actionOnStopSpeaking),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Config::bientityActionOnSpeak),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action_stop", BiEntityAction.CODEC).forGetter(Config::bientityActionOnStopSpeaking),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Config::bientityCondition)
        ).apply(i, Config::new));
    }
}
