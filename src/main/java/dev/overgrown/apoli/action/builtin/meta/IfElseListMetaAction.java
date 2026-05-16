package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public final class IfElseListMetaAction<CTX, COND, ACTION> implements ActionType<CTX, IfElseListMetaAction.Cfg<COND, ACTION>> {
    private final Codec<COND> condCodec;
    private final Codec<ACTION> actionCodec;
    private final BiPredicate<COND, CTX> condTester;
    private final BiConsumer<ACTION, CTX> actionRunner;

    public IfElseListMetaAction(Codec<COND> condCodec, Codec<ACTION> actionCodec,
                                BiPredicate<COND, CTX> condTester, BiConsumer<ACTION, CTX> actionRunner) {
        this.condCodec = condCodec;
        this.actionCodec = actionCodec;
        this.condTester = condTester;
        this.actionRunner = actionRunner;
    }

    public record Branch<COND, ACTION>(COND condition, ACTION action) {}
    public record Cfg<COND, ACTION>(List<Branch<COND, ACTION>> actions) {}

    @Override
    public MapCodec<Cfg<COND, ACTION>> codec() {
        Codec<Branch<COND, ACTION>> branchCodec = RecordCodecBuilder.create(i -> i.group(
            condCodec.fieldOf("condition").forGetter(Branch<COND, ACTION>::condition),
            actionCodec.fieldOf("action").forGetter(Branch<COND, ACTION>::action)
        ).apply(i, Branch::new));
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.list(branchCodec).fieldOf("actions").forGetter(Cfg<COND, ACTION>::actions)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg<COND, ACTION> cfg, CTX ctx) {
        for (Branch<COND, ACTION> branch : cfg.actions) {
            if (condTester.test(branch.condition, ctx)) {
                actionRunner.accept(branch.action, ctx);
                return;
            }
        }
    }
}