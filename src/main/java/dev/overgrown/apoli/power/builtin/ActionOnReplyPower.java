package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class ActionOnReplyPower extends PowerType<ActionOnReplyPower.Config> {
    public record Config(Optional<BiEntityAction> bientityAction, int window, double range) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.optionalFieldOf("bientity_action").forGetter(Config::bientityAction),
            Codec.INT.optionalFieldOf("window", 60).forGetter(Config::window),
            Codec.DOUBLE.optionalFieldOf("range", 16.0).forGetter(Config::range)
        ).apply(i, Config::new));
    }
}
