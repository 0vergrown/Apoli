package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.power.builtin.ModifyEnchantmentLevelHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
        int compareTo,
        Optional<ResourceKey<Enchantment>> key
    ) {
        public Cfg(Optional<ResourceLocation> enchantment, boolean useModifications, Comparison comparison, int compareTo) {
            this(enchantment, useModifications, comparison, compareTo,
                enchantment.map(id -> ResourceKey.create(Registries.ENCHANTMENT, id)));
        }
    }

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
        if (cfg.key.isPresent()) {
            Holder<Enchantment> holder = ctx.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(cfg.key.get())
                .orElse(null);
            if (holder == null) {
                value = 0;
            } else if (cfg.useModifications) {
                value = ModifyEnchantmentLevelHandler.levelInContext(ctx.holder(), stack, holder,
                    EnchantmentHelper.getItemEnchantmentLevel(holder, stack));
            } else {
                value = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(holder);
            }
        } else {
            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
            if (cfg.useModifications) {
                enchantments = ModifyEnchantmentLevelHandler.enchantmentsInContext(ctx.holder(), stack, enchantments);
            }
            value = enchantments.size();
        }
        return cfg.comparison.compare(value, cfg.compareTo);
    }
}
