package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public final class FuelItemCondition implements ConditionType<ItemCtx, FuelItemCondition.Cfg> {
    public record Cfg(Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER).forGetter(Cfg::comparison),
            Codec.INT.optionalFieldOf("compare_to", 0).forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        int burnTime = AbstractFurnaceBlockEntity.getFuel().getOrDefault(ctx.stack().getItem(), 0);
        return cfg.comparison.compare(burnTime, cfg.compareTo);
    }
}
