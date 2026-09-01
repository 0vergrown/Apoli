package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BlockAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.PowerType;

import java.util.List;
import java.util.Optional;

public final class ModifyBouncinessPower extends PowerType<ModifyBouncinessPower.Config> {
    public record Config(
        Optional<BlockCondition> blockCondition,
        Optional<BlockAction> blockAction,
        Optional<EntityAction> entityAction,
        Optional<List<AttributeModifier>> modifiers,
        Optional<AttributeModifier> modifier,
        boolean damage,
        boolean preventable
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                LoggedOptionalField.of("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
                LoggedOptionalField.of("block_action", BlockAction.CODEC).forGetter(Config::blockAction),
                LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
                AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
                AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
                Codec.BOOL.optionalFieldOf("damage", false).forGetter(Config::damage),
                Codec.BOOL.optionalFieldOf("preventable", true).forGetter(Config::preventable)
        ).apply(i, Config::new));
    }
}
