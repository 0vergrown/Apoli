package dev.overgrown.apoli.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class EnchantmentLevels {

    private final ResourceLocation id;
    private volatile Enchantment enchantment;

    private EnchantmentLevels(ResourceLocation id) {
        this.id = id;
    }

    public static EnchantmentLevels of(ResourceLocation id) {
        return new EnchantmentLevels(id);
    }

    public int levelIn(@Nullable Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) return 0;
        Enchantment resolved = enchantment;
        if (resolved == null) {
            resolved = BuiltInRegistries.ENCHANTMENT.get(id);
            if (resolved == null) return 0;
            enchantment = resolved;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(resolved, stack);
    }
}
