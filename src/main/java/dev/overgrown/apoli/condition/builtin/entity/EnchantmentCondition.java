package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class EnchantmentCondition implements ConditionType<EntityCtx, EnchantmentCondition.Cfg> {
    public enum Calculation implements StringRepresentable {
        SUM("sum"),
        MAX("max");
        public static final Codec<Calculation> CODEC = StringRepresentable.fromEnum(Calculation::values);
        private final String name;
        Calculation(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    public record Cfg(ResourceLocation enchantment, Calculation calculation, Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("enchantment").forGetter(Cfg::enchantment),
            Calculation.CODEC.optionalFieldOf("calculation", Calculation.SUM).forGetter(Cfg::calculation),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, cfg.enchantment);
        Holder<Enchantment> holder = ctx.level().registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolder(key)
            .orElse(null);
        if (holder == null) return false;
        LivingEntity living = ctx.living();
        if (living == null) return false;
        int sum = 0, max = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(holder, living.getItemBySlot(slot));
            sum += level;
            if (level > max) max = level;
        }
        int val = cfg.calculation == Calculation.SUM ? sum : max;
        return cfg.comparison.compare(val, cfg.compareTo);
    }
}
