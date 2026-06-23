package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

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
            ResourceLocation.CODEC.optionalFieldOf("enchantment").forGetter(Cfg::enchantment),
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
            Optional<Holder.Reference<Enchantment>> holder = ctx.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, cfg.enchantment.get()));
            value = holder.map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
        } else {
            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
            value = enchantments.size();
        }
        return cfg.comparison.compare(value, cfg.compareTo);
    }
}
