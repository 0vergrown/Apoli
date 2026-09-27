package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.power.builtin.ModifyEnchantmentLevelHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;
import java.util.Optional;

public final class EnchantmentCondition implements ConditionType<ItemCtx, EnchantmentCondition.Cfg> {
    public record Cfg(
        Optional<ResourceLocation> enchantment,
        boolean useModifications,
        Comparison comparison,
        int compareTo
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("enchantment").forGetter(Cfg::enchantment),
            Codec.BOOL.optionalFieldOf("use_modifications", true).forGetter(Cfg::useModifications),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER).forGetter(Cfg::comparison),
            Codec.INT.optionalFieldOf("compare_to", 0).forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        ItemStack stack = ctx.stack() == null ? ItemStack.EMPTY : ctx.stack();
        int value;
        if (cfg.enchantment.isPresent()) {
            Enchantment ench = BuiltInRegistries.ENCHANTMENT.get(cfg.enchantment.get());
            if (ench == null) {
                value = 0;
            } else if (cfg.useModifications) {
                value = ModifyEnchantmentLevelHandler.levelInContext(ctx.holder(), stack, ench,
                    EnchantmentHelper.getItemEnchantmentLevel(ench, stack));
            } else {
                value = ModifyEnchantmentLevelHandler.rawLevel(stack, ench);
            }
        } else {
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
            if (cfg.useModifications) {
                enchantments = ModifyEnchantmentLevelHandler.enchantmentsInContext(ctx.holder(), stack, enchantments);
            }
            value = enchantments.size();
        }
        return cfg.comparison.compare(value, cfg.compareTo);
    }
}
