package dev.overgrown.apoli.condition.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;

public final class RandomChanceMeta<CTX> implements ConditionType<CTX, RandomChanceMeta.Cfg> {
    public record Cfg(float chance) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("chance").forGetter(Cfg::chance)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, CTX ctx) {
        return Math.random() < cfg.chance;
    }
}