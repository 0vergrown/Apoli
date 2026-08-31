package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class PreventTeleportPower extends PowerType<PreventTeleportPower.Config> {
    public record Config(Optional<EntityAction> entityAction, boolean preventDimensionChange) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            Codec.BOOL.optionalFieldOf("prevent_dimension_change", true).forGetter(Config::preventDimensionChange)
        ).apply(i, Config::new));
    }
}
