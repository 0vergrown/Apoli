package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

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
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        ItemStack stack = ctx.stack();
        if (stack == null || stack.isEmpty()) return cfg.comparison.compare(0, cfg.compareTo);
        int value;
        if (cfg.enchantment.isPresent()) {
            Enchantment ench = BuiltInRegistries.ENCHANTMENT.get(cfg.enchantment.get());
            value = ench == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(ench, stack);
        } else {
            value = EnchantmentHelper.getEnchantments(stack).size();
        }
        return cfg.comparison.compare(value, cfg.compareTo);
    }
}
