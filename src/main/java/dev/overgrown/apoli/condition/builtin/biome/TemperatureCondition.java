package dev.overgrown.apoli.condition.builtin.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiomeCtx;
import dev.overgrown.apoli.data.Comparison;

public final class TemperatureCondition implements ConditionType<BiomeCtx, TemperatureCondition.Cfg> {
    public record Cfg(Comparison comparison, float compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.FLOAT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BiomeCtx ctx) {
        return cfg.comparison.compare(ctx.biome().value().getBaseTemperature(), cfg.compareTo);
    }
}
