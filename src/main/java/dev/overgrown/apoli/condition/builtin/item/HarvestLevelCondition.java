package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;

public final class HarvestLevelCondition implements ConditionType<ItemCtx, HarvestLevelCondition.Cfg> {
    public record Cfg(Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        ItemStack stack = ctx.stack();
        int level = 0;
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof TieredItem tiered) {
            level = levelFor(tiered.getTier());
        }
        return cfg.comparison.compare(level, cfg.compareTo);
    }

    private static int levelFor(Tier tier) {
        if (tier instanceof Tiers t) {
            return switch (t) {
                case WOOD, GOLD -> 0;
                case STONE -> 1;
                case IRON -> 2;
                case DIAMOND -> 3;
                case NETHERITE -> 4;
            };
        }
        float speed = tier.getSpeed();
        if (speed >= 9f) return 4;
        if (speed >= 8f) return 3;
        if (speed >= 6f) return 2;
        if (speed >= 4f) return 1;
        return 0;
    }
}
