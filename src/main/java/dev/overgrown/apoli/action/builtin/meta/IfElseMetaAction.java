package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public final class IfElseMetaAction<CTX, COND, ACTION> implements ActionType<CTX, IfElseMetaAction.Cfg<COND, ACTION>> {
    private final Codec<COND> condCodec;
    private final Codec<ACTION> actionCodec;
    private final BiPredicate<COND, CTX> condTester;
    private final BiConsumer<ACTION, CTX> actionRunner;

    public IfElseMetaAction(Codec<COND> condCodec, Codec<ACTION> actionCodec,
                            BiPredicate<COND, CTX> condTester, BiConsumer<ACTION, CTX> actionRunner) {
        this.condCodec = condCodec;
        this.actionCodec = actionCodec;
        this.condTester = condTester;
        this.actionRunner = actionRunner;
    }

    public record Cfg<COND, ACTION>(COND condition, ACTION ifAction, Optional<ACTION> elseAction) {}

    @Override
    public MapCodec<Cfg<COND, ACTION>> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            condCodec.fieldOf("condition").forGetter(Cfg<COND, ACTION>::condition),
            actionCodec.fieldOf("if_action").forGetter(Cfg<COND, ACTION>::ifAction),
            actionCodec.optionalFieldOf("else_action").forGetter(Cfg<COND, ACTION>::elseAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg<COND, ACTION> cfg, CTX ctx) {
        if (condTester.test(cfg.condition, ctx)) actionRunner.accept(cfg.ifAction, ctx);
        else cfg.elseAction.ifPresent(a -> actionRunner.accept(a, ctx));
    }
}
