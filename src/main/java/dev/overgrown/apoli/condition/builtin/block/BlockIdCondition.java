package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class BlockIdCondition implements ConditionType<BlockCtx, BlockIdCondition.Cfg> {
    public record Cfg(ResourceLocation block) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("block").forGetter(Cfg::block)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        Block expected = BuiltInRegistries.BLOCK.get(cfg.block);
        return expected != null && ctx.state().is(expected);
    }
}
