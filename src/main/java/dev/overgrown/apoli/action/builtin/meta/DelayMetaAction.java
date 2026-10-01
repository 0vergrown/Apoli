package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.DelayedActionQueue;

import dev.overgrown.apoli.data.Expression;
import net.minecraft.world.entity.Entity;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class DelayMetaAction<CTX, W> implements ActionType<CTX, DelayMetaAction.Cfg<W>> {
    private final Codec<W> wrapperCodec;
    private final BiConsumer<W, CTX> runner;
    private final Predicate<CTX> aliveCheck;
    private final Function<CTX, Entity> subject;

    public DelayMetaAction(Codec<W> wrapperCodec, BiConsumer<W, CTX> runner, Predicate<CTX> aliveCheck,
                           Function<CTX, Entity> subject) {
        this.wrapperCodec = wrapperCodec;
        this.runner = runner;
        this.aliveCheck = aliveCheck;
        this.subject = subject;
    }

    public record Cfg<W>(Expression ticks, W action) {}

    @Override
    public MapCodec<Cfg<W>> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.INT_OR_EXPR.fieldOf("ticks").forGetter(Cfg<W>::ticks),
            wrapperCodec.fieldOf("action").forGetter(Cfg<W>::action)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg<W> cfg, CTX ctx) {
        int ticks = cfg.ticks.evalInt(subject.apply(ctx));
        if (ticks <= 0) {
            runner.accept(cfg.action, ctx);
        } else {
            DelayedActionQueue.schedule(ticks, () -> aliveCheck.test(ctx),
                () -> runner.accept(cfg.action, ctx));
        }
    }
}
