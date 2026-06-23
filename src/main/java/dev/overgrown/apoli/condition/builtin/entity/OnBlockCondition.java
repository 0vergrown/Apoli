package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.BlockPos;

import java.util.Optional;

public final class OnBlockCondition implements ConditionType<EntityCtx, OnBlockCondition.Cfg> {
    public record Cfg(Optional<BlockCondition> blockCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Cfg::blockCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!ctx.entity().onGround()) return false;
        if (cfg.blockCondition.isEmpty()) return true;
        BlockPos below = ctx.entity().blockPosition().below();
        return cfg.blockCondition.get().test(new BlockCtx(below, ctx.level().getBlockState(below), ctx.level()));
    }
}
