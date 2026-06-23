package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class PreventElytraFlightPower extends PowerType<PreventElytraFlightPower.Config> {
    public record Config(Optional<EntityAction> entityAction) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction)
        ).apply(i, Config::new));
    }
}
