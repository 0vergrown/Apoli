package dev.overgrown.apoli.action.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RemoveEnchantmentItemAction implements ActionType<ItemCtx, RemoveEnchantmentItemAction.Cfg> {
    public record Cfg(
        Optional<ResourceLocation> enchantment,
        Optional<List<ResourceLocation>> enchantments,
        Optional<Integer> levels,
        boolean resetRepairCost
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("enchantment").forGetter(Cfg::enchantment),
            IdCodecs.ID.listOf().optionalFieldOf("enchantments").forGetter(Cfg::enchantments),
            Codec.INT.optionalFieldOf("levels").forGetter(Cfg::levels),
            Codec.BOOL.optionalFieldOf("reset_repair_cost", false).forGetter(Cfg::resetRepairCost)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, ItemCtx ctx) {
        ItemStack stack = ctx.stack();
        if (stack == null || stack.isEmpty()) return;
        Map<Enchantment, Integer> current = EnchantmentHelper.getEnchantments(stack);
        boolean removeAll = cfg.enchantment.isEmpty() && cfg.enchantments.isEmpty();
        Map<Enchantment, Integer> remaining = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : current.entrySet()) {
            ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(entry.getKey());
            boolean targeted = removeAll
                || (cfg.enchantment.isPresent() && cfg.enchantment.get().equals(id))
                || (cfg.enchantments.isPresent() && cfg.enchantments.get().contains(id));
            if (targeted && (cfg.levels.isEmpty() || cfg.levels.get().equals(entry.getValue()))) {
                continue;
            }
            remaining.put(entry.getKey(), entry.getValue());
        }
        EnchantmentHelper.setEnchantments(remaining, stack);
        if (cfg.resetRepairCost && stack.getTag() != null) {
            stack.getTag().remove("RepairCost");
        }
    }
}
