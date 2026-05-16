package dev.overgrown.apoli.condition.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;

import java.util.List;
import java.util.function.BiPredicate;

public final class AnyOfMeta<CTX, W> implements ConditionType<CTX, AnyOfMeta.Cfg<W>> {
    private final Codec<W> wrapperCodec;
    private final BiPredicate<W, CTX> tester;

    public AnyOfMeta(Codec<W> wrapperCodec, BiPredicate<W, CTX> tester) {
        this.wrapperCodec = wrapperCodec;
        this.tester = tester;
    }

    public record Cfg<W>(List<W> conditions) {}

    @Override
    public MapCodec<Cfg<W>> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.list(wrapperCodec).fieldOf("conditions").forGetter(Cfg<W>::conditions)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg<W> cfg, CTX ctx) {
        for (W w : cfg.conditions) if (tester.test(w, ctx)) return true;
        return false;
    }
}