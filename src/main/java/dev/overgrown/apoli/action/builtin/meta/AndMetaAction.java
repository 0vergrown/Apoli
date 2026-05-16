package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;

import java.util.List;
import java.util.function.BiConsumer;

public final class AndMetaAction<CTX, W> implements ActionType<CTX, AndMetaAction.Cfg<W>> {
    private final Codec<W> wrapperCodec;
    private final BiConsumer<W, CTX> runner;

    public AndMetaAction(Codec<W> wrapperCodec, BiConsumer<W, CTX> runner) {
        this.wrapperCodec = wrapperCodec;
        this.runner = runner;
    }

    public record Cfg<W>(List<W> actions) {}

    @Override
    public MapCodec<Cfg<W>> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.list(wrapperCodec).fieldOf("actions").forGetter(Cfg<W>::actions)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg<W> cfg, CTX ctx) {
        for (W w : cfg.actions) runner.accept(w, ctx);
    }
}