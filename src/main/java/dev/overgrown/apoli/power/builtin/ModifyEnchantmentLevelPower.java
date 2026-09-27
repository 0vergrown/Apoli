package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class ModifyEnchantmentLevelPower extends PowerType<ModifyEnchantmentLevelPower.Config> {
    public record Config(
        ResourceLocation enchantment,
        Optional<ItemCondition> itemCondition,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers,
        List<AttributeModifier> allModifiers
    ) {
        public Config(ResourceLocation enchantment, Optional<ItemCondition> itemCondition,
                      Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers) {
            this(enchantment, itemCondition, modifier, modifiers, AttributeModifierHelper.flatten(modifier, modifiers));
        }

        public boolean appliesTo(ItemStack stack, LivingEntity holder) {
            return itemCondition.isEmpty() || itemCondition.get().test(new ItemCtx(stack, holder.level(), holder));
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("enchantment").forGetter(Config::enchantment),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Config::itemCondition),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }
}
