package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.IdOrTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

public final class BlockIdCondition implements ConditionType<BlockCtx, BlockIdCondition.Cfg> {
    public record Cfg(IdOrTag<Block> block) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdOrTag.codec(Registries.BLOCK).fieldOf("block").forGetter(Cfg::block)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        return cfg.block.matches(ctx.state().getBlockHolder());
    }
}
