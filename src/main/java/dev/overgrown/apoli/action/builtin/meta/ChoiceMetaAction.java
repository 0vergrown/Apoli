package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;

import java.util.List;
import java.util.function.BiConsumer;

public final class ChoiceMetaAction<CTX, W> implements ActionType<CTX, ChoiceMetaAction.Cfg<W>> {
    private final Codec<W> wrapperCodec;
    private final BiConsumer<W, CTX> runner;

    public ChoiceMetaAction(Codec<W> wrapperCodec, BiConsumer<W, CTX> runner) {
        this.wrapperCodec = wrapperCodec;
        this.runner = runner;
    }

    public record Weighted<W>(int weight, W element) {}
    public record Cfg<W>(List<Weighted<W>> actions) {}

    @Override
    public MapCodec<Cfg<W>> codec() {
        Codec<Weighted<W>> weightedCodec = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("weight").forGetter(Weighted<W>::weight),
            wrapperCodec.fieldOf("element").forGetter(Weighted<W>::element)
        ).apply(i, Weighted::new));
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.list(weightedCodec).fieldOf("actions").forGetter(Cfg<W>::actions)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg<W> cfg, CTX ctx) {
        int total = 0;
        for (Weighted<W> w : cfg.actions) total += Math.max(0, w.weight);
        if (total <= 0) return;
        int roll = (int) Math.floor(Math.random() * total);
        for (Weighted<W> w : cfg.actions) {
            roll -= Math.max(0, w.weight);
            if (roll < 0) { runner.accept(w.element, ctx); return; }
        }
    }
}
