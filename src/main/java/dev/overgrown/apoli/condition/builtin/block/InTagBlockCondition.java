package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class InTagBlockCondition implements ConditionType<BlockCtx, InTagBlockCondition.Cfg> {
    public record Cfg(TagKey<Block> tag) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.<Block>tagKey(Registries.BLOCK).fieldOf("tag").forGetter(Cfg::tag)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        return ctx.state().is(cfg.tag);
    }
}
