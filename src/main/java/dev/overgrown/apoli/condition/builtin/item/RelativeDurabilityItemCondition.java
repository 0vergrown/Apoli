package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.item.ItemStack;

public final class RelativeDurabilityItemCondition implements ConditionType<ItemCtx, RelativeDurabilityItemCondition.Cfg> {
    public record Cfg(Comparison comparison, float compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.FLOAT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        ItemStack stack = ctx.stack();
        float fraction = 1f;
        if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            fraction = (stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage();
        }
        return cfg.comparison.compare(fraction, cfg.compareTo);
    }
}
