package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Nbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

public final class NbtCondition implements ConditionType<EntityCtx, NbtCondition.Cfg> {
    public record Cfg(Nbt nbt) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Nbt.CODEC.fieldOf("nbt").forGetter(Cfg::nbt)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        CompoundTag actual = EntityNbtSnapshot.of(ctx.raw(), cfg.nbt.tag());
        return NbtUtils.compareNbt(cfg.nbt.tag(), actual, true);
    }
}
