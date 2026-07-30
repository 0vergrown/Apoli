package dev.overgrown.apoli.compat.voicechat;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class ActionOnSpeakPower extends PowerType<ActionOnSpeakPower.Config> {
    public record Config(Optional<EntityAction> actionOnSpeak, Optional<EntityAction> actionOnStopSpeaking) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::actionOnSpeak),
            EntityAction.CODEC.optionalFieldOf("entity_action_stop").forGetter(Config::actionOnStopSpeaking)
        ).apply(i, Config::new));
    }
}
