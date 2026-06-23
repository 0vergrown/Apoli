package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public final class ModifyStatusEffectAmplifierPower extends PowerType<ModifyStatusEffectAmplifierPower.Config> {
    public record Config(
        Optional<ResourceLocation> statusEffect,
        Optional<List<ResourceLocation>> statusEffects,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("status_effect").forGetter(Config::statusEffect),
            ResourceLocation.CODEC.listOf().optionalFieldOf("status_effects").forGetter(Config::statusEffects),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }
}
