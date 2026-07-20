package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Nbt;
import dev.overgrown.apoli.data.NbtComparison;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public final class NbtItemCondition implements ConditionType<ItemCtx, NbtItemCondition.Cfg> {
    public record Cfg(Nbt nbt) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Nbt.CODEC.fieldOf("nbt").forGetter(Cfg::nbt)
        ).apply(i, Cfg::new));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean test(Cfg cfg, ItemCtx ctx) {
        CustomData data = ctx.stack().get(DataComponents.CUSTOM_DATA);
        if (data == null) return cfg.nbt.tag().isEmpty();
        return NbtComparison.matches(cfg.nbt.tag(), data.getUnsafe());
    }
}
