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
        List<Branch<COND, ACTION>> branches = cfg.actions;
        int size = branches.size();
        if (size == 0) return;

        if (size <= Long.SIZE) {
            long matched = 0L;
            for (int i = 0; i < size; i++) {
                if (condTester.test(branches.get(i).condition, ctx)) matched |= 1L << i;
            }
            if (matched == 0L) return;
            for (int i = 0; i < size; i++) {
                if ((matched & (1L << i)) != 0L) actionRunner.accept(branches.get(i).action, ctx);
            }
            return;
        }

        boolean[] matched = new boolean[size];
        for (int i = 0; i < size; i++) {
            matched[i] = condTester.test(branches.get(i).condition, ctx);
        }
        for (int i = 0; i < size; i++) {
            if (matched[i]) actionRunner.accept(branches.get(i).action, ctx);
        }
    }
}
