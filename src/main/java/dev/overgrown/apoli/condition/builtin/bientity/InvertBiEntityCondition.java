package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;

public final class InvertBiEntityCondition implements ConditionType<BiEntityCtx, InvertBiEntityCondition.Cfg> {
    public record Cfg(BiEntityCondition condition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityCondition.CODEC.fieldOf("condition").forGetter(Cfg::condition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BiEntityCtx ctx) {
        return cfg.condition.test(ctx.swap());
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
