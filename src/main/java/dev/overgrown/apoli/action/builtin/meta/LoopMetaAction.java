package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.DelayedActionQueue;

import java.util.Optional;
import java.util.function.BiConsumer;

public final class LoopMetaAction<CTX, W> implements ActionType<CTX, LoopMetaAction.Cfg<W>> {

    private final Codec<W> wrapperCodec;
    private final BiConsumer<W, CTX> runner;

    public LoopMetaAction(Codec<W> wrapperCodec, BiConsumer<W, CTX> runner) {
        this.wrapperCodec = wrapperCodec;
        this.runner = runner;
    }

    public record Cfg<W>(int value, int ticks, Optional<W> beforeAction, Optional<W> action, Optional<W> afterAction) {}

    @Override
    public MapCodec<Cfg<W>> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("value", 1).forGetter(Cfg<W>::value),
            Codec.INT.optionalFieldOf("ticks", 1).forGetter(Cfg<W>::ticks),
            wrapperCodec.optionalFieldOf("before_action").forGetter(Cfg<W>::beforeAction),
            wrapperCodec.optionalFieldOf("action").forGetter(Cfg<W>::action),
            wrapperCodec.optionalFieldOf("after_action").forGetter(Cfg<W>::afterAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg<W> cfg, CTX ctx) {
        int iterations = cfg.value;
        if (iterations <= 0) return;

        int interval = Math.max(1, cfg.ticks);

        cfg.beforeAction.ifPresent(a -> runner.accept(a, ctx));

        if (cfg.action.isEmpty()) {
            cfg.afterAction.ifPresent(a -> runner.accept(a, ctx));
            return;
        }

        W body = cfg.action.get();
        runner.accept(body, ctx);

        if (iterations == 1) {
            cfg.afterAction.ifPresent(a -> runner.accept(a, ctx));
        } else {
            arm(iterations - 1, interval, body, cfg.afterAction, ctx);
        }
    }

    private void arm(int remaining, int interval, W body, Optional<W> afterAction, CTX ctx) {
        DelayedActionQueue.schedule(interval, () -> {
            runner.accept(body, ctx);
            if (remaining > 1) {
                arm(remaining - 1, interval, body, afterAction, ctx);
            } else {
                afterAction.ifPresent(a -> runner.accept(a, ctx));
            }
        });
    }
}
