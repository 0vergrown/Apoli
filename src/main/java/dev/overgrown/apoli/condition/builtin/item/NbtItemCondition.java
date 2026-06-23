package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Nbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

public final class NbtItemCondition implements ConditionType<ItemCtx, NbtItemCondition.Cfg> {
    public record Cfg(Nbt nbt) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Nbt.CODEC.fieldOf("nbt").forGetter(Cfg::nbt)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        CompoundTag tag = ctx.stack().getTag();
        if (tag == null) return cfg.nbt.tag().isEmpty();
        return NbtUtils.compareNbt(cfg.nbt.tag(), tag, true);
    }
}
