package dev.overgrown.apoli.data;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class EnchantmentLevels {

    private record Resolved(Registry<Enchantment> registry, @Nullable Holder<Enchantment> holder) {}

    private final ResourceKey<Enchantment> key;
    private volatile Resolved resolved;

    private EnchantmentLevels(ResourceKey<Enchantment> key) {
        this.key = key;
    }

    public static EnchantmentLevels of(ResourceLocation id) {
        return new EnchantmentLevels(ResourceKey.create(Registries.ENCHANTMENT, id));
    }

    public int levelIn(@Nullable Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) return 0;
        Holder<Enchantment> holder = holder(level);
        return holder == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }

    @Nullable
    private Holder<Enchantment> holder(Level level) {
        Registry<Enchantment> registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Resolved current = resolved;
        if (current == null || current.registry() != registry) {
            current = new Resolved(registry, registry.getHolder(key).orElse(null));
            resolved = current;
        }
        return current.holder();
    }
}
