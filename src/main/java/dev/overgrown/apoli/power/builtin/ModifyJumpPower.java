package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.PowerType;

import java.util.List;
import java.util.Optional;

public final class ModifyJumpPower extends PowerType<ModifyJumpPower.Config> {
    public record Config(
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers,
        Optional<EntityAction> entityAction
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction)
        ).apply(i, Config::new));
    }
}
