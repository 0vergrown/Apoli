package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Nbt;
import dev.overgrown.apoli.data.NbtComparison;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class NbtBlockCondition implements ConditionType<BlockCtx, NbtBlockCondition.Cfg> {
    public record Cfg(Nbt nbt) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Nbt.CODEC.fieldOf("nbt").forGetter(Cfg::nbt)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        BlockEntity be = ctx.level().getBlockEntity(ctx.pos());
        if (be == null) return false;
        return NbtComparison.matches(cfg.nbt.tag(), be.saveWithoutMetadata(ctx.level().registryAccess()));
    }
}
