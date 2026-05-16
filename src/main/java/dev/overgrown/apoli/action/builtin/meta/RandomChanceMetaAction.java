package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;

import java.util.Optional;
import java.util.function.BiConsumer;

public final class RandomChanceMetaAction<CTX, W> implements ActionType<CTX, RandomChanceMetaAction.Cfg<W>> {
    private final Codec<W> wrapperCodec;
    private final BiConsumer<W, CTX> runner;

    public RandomChanceMetaAction(Codec<W> wrapperCodec, BiConsumer<W, CTX> runner) {
        this.wrapperCodec = wrapperCodec;
        this.runner = runner;
    }

    public record Cfg<W>(float chance, W action, Optional<W> failAction) {}

    @Override
    public MapCodec<Cfg<W>> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("chance").forGetter(Cfg<W>::chance),
            wrapperCodec.fieldOf("action").forGetter(Cfg<W>::action),
            wrapperCodec.optionalFieldOf("fail_action").forGetter(Cfg<W>::failAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg<W> cfg, CTX ctx) {
        if (Math.random() < cfg.chance) runner.accept(cfg.action, ctx);
        else cfg.failAction.ifPresent(a -> runner.accept(a, ctx));
    }
}