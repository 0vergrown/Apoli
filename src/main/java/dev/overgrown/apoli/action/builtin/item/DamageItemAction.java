package dev.overgrown.apoli.action.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Optional;

public final class DamageItemAction implements ActionType<ItemCtx, DamageItemAction.Cfg> {
    public record Cfg(int amount, boolean ignoreUnbreaking) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("amount", 1).forGetter(Cfg::amount),
            Codec.BOOL.optionalFieldOf("ignore_unbreaking", false).forGetter(Cfg::ignoreUnbreaking)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, ItemCtx ctx) {
        ItemStack stack = ctx.stack();
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem() || cfg.amount <= 0) return;
        int amount = cfg.amount;
        if (!cfg.ignoreUnbreaking) {
            Optional<Holder.Reference<Enchantment>> unbreakingHolder = ctx.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(Enchantments.UNBREAKING);
            int unbreaking = unbreakingHolder.map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
            int kept = 0;
            for (int j = 0; j < amount; j++) {
                if (unbreaking > 0 && ctx.level().random.nextInt(unbreaking + 1) > 0) continue;
                kept++;
            }
            amount = kept;
            if (amount <= 0) return;
        }
        int newDamage = stack.getDamageValue() + amount;
        if (newDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
        } else {
            stack.setDamageValue(newDamage);
        }
    }
}
