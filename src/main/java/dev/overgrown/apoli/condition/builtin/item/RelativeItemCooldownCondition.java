package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class RelativeItemCooldownCondition implements ConditionType<ItemCtx, RelativeItemCooldownCondition.Cfg> {
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
        if (stack == null || stack.isEmpty()) return false;
        if (!(ctx.holder() instanceof Player player)) return false;
        return cfg.comparison.compare(player.getCooldowns().getCooldownPercent(stack.getItem(), 0f), cfg.compareTo);
    }
}
