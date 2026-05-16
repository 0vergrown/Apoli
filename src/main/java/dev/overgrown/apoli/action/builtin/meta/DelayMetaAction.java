package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.DelayedActionQueue;

import java.util.function.BiConsumer;

public final class DelayMetaAction<CTX, W> implements ActionType<CTX, DelayMetaAction.Cfg<W>> {
    private final Codec<W> wrapperCodec;
    private final BiConsumer<W, CTX> runner;

    public DelayMetaAction(Codec<W> wrapperCodec, BiConsumer<W, CTX> runner) {
        this.wrapperCodec = wrapperCodec;
        this.runner = runner;
    }

    public record Cfg<W>(int ticks, W action) {}

    @Override
    public MapCodec<Cfg<W>> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("ticks").forGetter(Cfg<W>::ticks),
            wrapperCodec.fieldOf("action").forGetter(Cfg<W>::action)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg<W> cfg, CTX ctx) {
        if (cfg.ticks <= 0) {
            runner.accept(cfg.action, ctx);
        } else {
            DelayedActionQueue.schedule(cfg.ticks, () -> runner.accept(cfg.action, ctx));
        }
    }
}