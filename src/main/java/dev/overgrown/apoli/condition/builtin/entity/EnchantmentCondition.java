package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.power.builtin.ModifyEnchantmentLevelHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class EnchantmentCondition implements ConditionType<EntityCtx, EnchantmentCondition.Cfg> {
    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();

    public enum Calculation implements StringRepresentable {
        SUM("sum"),
        MAX("max");
        public static final Codec<Calculation> CODEC = StringRepresentable.fromEnum(Calculation::values);
        private final String name;
        Calculation(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    public record Cfg(ResourceLocation enchantment, boolean useModifications, Calculation calculation,
                      Comparison comparison, int compareTo, ResourceKey<Enchantment> key) {
        public Cfg(ResourceLocation enchantment, boolean useModifications, Calculation calculation,
                   Comparison comparison, int compareTo) {
            this(enchantment, useModifications, calculation, comparison, compareTo,
                ResourceKey.create(Registries.ENCHANTMENT, enchantment));
        }
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("enchantment").forGetter(Cfg::enchantment),
            Codec.BOOL.optionalFieldOf("use_modifications", true).forGetter(Cfg::useModifications),
            Calculation.CODEC.optionalFieldOf("calculation", Calculation.SUM).forGetter(Cfg::calculation),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Holder<Enchantment> holder = ctx.level().registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolder(cfg.key)
            .orElse(null);
        if (holder == null) return false;
        LivingEntity living = ctx.living();
        if (living == null) return false;
        int sum = 0, max = 0;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = living.getItemBySlot(slot);
            int level;
            if (!cfg.useModifications) {
                level = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(holder);
            } else if (!stack.isEmpty()) {
                level = EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
            } else if (ModifyEnchantmentLevelHandler.isHolder(living) && holder.value().matchingSlot(slot)) {
                level = ModifyEnchantmentLevelHandler.levelInContext(living, stack, holder, 0);
            } else {
                level = 0;
            }
            sum += level;
            if (level > max) max = level;
        }
        int val = cfg.calculation == Calculation.SUM ? sum : max;
        return cfg.comparison.compare(val, cfg.compareTo);
    }
}
