package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public final class ModifyEnchantmentLevelPower extends PowerType<ModifyEnchantmentLevelPower.Config> {
    public record Config(
        ResourceLocation enchantment,
        Optional<ItemCondition> itemCondition,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("enchantment").forGetter(Config::enchantment),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Config::itemCondition),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }
}
