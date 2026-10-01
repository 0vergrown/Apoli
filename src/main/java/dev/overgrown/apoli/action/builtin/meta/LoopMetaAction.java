package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.DelayedActionQueue;

import dev.overgrown.apoli.data.Expression;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class LoopMetaAction<CTX, W> implements ActionType<CTX, LoopMetaAction.Cfg<W>> {
    private final Codec<W> wrapperCodec;
    private final BiConsumer<W, CTX> runner;
    private final Predicate<CTX> aliveCheck;
    private final Function<CTX, Entity> subject;

    public LoopMetaAction(Codec<W> wrapperCodec, BiConsumer<W, CTX> runner, Predicate<CTX> aliveCheck,
                          Function<CTX, Entity> subject) {
        this.wrapperCodec = wrapperCodec;
        this.runner = runner;
        this.aliveCheck = aliveCheck;
        this.subject = subject;
    }

    private static final Expression ONCE = Expression.constant(1);

    public record Cfg<W>(Expression value, Expression ticks, Optional<W> beforeAction, Optional<W> action,
                         Optional<W> afterAction) {}

    @Override
    public MapCodec<Cfg<W>> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.INT_OR_EXPR.optionalFieldOf("value", ONCE).forGetter(Cfg<W>::value),
            Expression.INT_OR_EXPR.optionalFieldOf("ticks", ONCE).forGetter(Cfg<W>::ticks),
            wrapperCodec.optionalFieldOf("before_action").forGetter(Cfg<W>::beforeAction),
            wrapperCodec.optionalFieldOf("action").forGetter(Cfg<W>::action),
            wrapperCodec.optionalFieldOf("after_action").forGetter(Cfg<W>::afterAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg<W> cfg, CTX ctx) {
        Entity entity = subject.apply(ctx);
        int iterations = cfg.value.evalInt(entity);
        if (iterations <= 0) return;
        int interval = cfg.ticks.evalInt(entity);

        cfg.beforeAction.ifPresent(a -> runner.accept(a, ctx));

        if (cfg.action.isEmpty()) {
            cfg.afterAction.ifPresent(a -> runner.accept(a, ctx));
            return;
        }

        W body = cfg.action.get();

        if (interval <= 0) {
            int count = Math.min(iterations, MAX_SYNC_ITERATIONS);
            for (int i = 0; i < count; i++) {
                runner.accept(body, ctx);
            }
            cfg.afterAction.ifPresent(a -> runner.accept(a, ctx));
            return;
        }

        runner.accept(body, ctx);

        if (iterations == 1) {
            cfg.afterAction.ifPresent(a -> runner.accept(a, ctx));
        } else {
            arm(iterations - 1, interval, body, cfg.afterAction, ctx);
        }
    }

    private static final int MAX_SYNC_ITERATIONS = 4096;

    private void arm(int remaining, int interval, W body, Optional<W> afterAction, CTX ctx) {
        DelayedActionQueue.schedule(interval, () -> aliveCheck.test(ctx), () -> {
            runner.accept(body, ctx);
            if (remaining > 1) {
                arm(remaining - 1, interval, body, afterAction, ctx);
            } else {
                afterAction.ifPresent(a -> runner.accept(a, ctx));
            }
        });
    }
}
