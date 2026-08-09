package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.DelayedActionQueue;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class DelayMetaAction<CTX, W> implements ActionType<CTX, DelayMetaAction.Cfg<W>> {
    private final Codec<W> wrapperCodec;
    private final BiConsumer<W, CTX> runner;
    private final Predicate<CTX> aliveCheck;

    public DelayMetaAction(Codec<W> wrapperCodec, BiConsumer<W, CTX> runner, Predicate<CTX> aliveCheck) {
        this.wrapperCodec = wrapperCodec;
        this.runner = runner;
        this.aliveCheck = aliveCheck;
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
            DelayedActionQueue.schedule(cfg.ticks, () -> aliveCheck.test(ctx),
                () -> runner.accept(cfg.action, ctx));
        }
    }
}
