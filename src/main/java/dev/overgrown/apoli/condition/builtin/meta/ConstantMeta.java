package dev.overgrown.apoli.condition.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;

public final class ConstantMeta<CTX> implements ConditionType<CTX, ConstantMeta.Cfg> {
    public record Cfg(boolean value) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("value").forGetter(Cfg::value)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, CTX ctx) {
        return cfg.value;
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
