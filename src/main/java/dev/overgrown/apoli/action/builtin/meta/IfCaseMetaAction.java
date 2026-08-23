package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public final class IfCaseMetaAction<CTX, COND, ACTION> implements ActionType<CTX, IfCaseMetaAction.Cfg<COND, ACTION>> {
    private final Codec<COND> condCodec;
    private final Codec<ACTION> actionCodec;
    private final BiPredicate<COND, CTX> condTester;
    private final BiConsumer<ACTION, CTX> actionRunner;

    public IfCaseMetaAction(Codec<COND> condCodec, Codec<ACTION> actionCodec,
                            BiPredicate<COND, CTX> condTester, BiConsumer<ACTION, CTX> actionRunner) {
        this.condCodec = condCodec;
        this.actionCodec = actionCodec;
        this.condTester = condTester;
        this.actionRunner = actionRunner;
    }

    public record Case<COND, ACTION>(COND condition, ACTION action) {}
    public record Cfg<COND, ACTION>(List<Case<COND, ACTION>> cases) {}

    @Override
    public MapCodec<Cfg<COND, ACTION>> codec() {
        Codec<Case<COND, ACTION>> caseCodec = RecordCodecBuilder.create(i -> i.group(
            condCodec.fieldOf("condition").forGetter(Case<COND, ACTION>::condition),
            actionCodec.fieldOf("action").forGetter(Case<COND, ACTION>::action)
        ).apply(i, Case::new));
        return dev.overgrown.apoli.alias.AliasingMapCodec.wrap(
            RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.list(caseCodec).fieldOf("cases").forGetter(Cfg<COND, ACTION>::cases)
            ).apply(i, Cfg::new)),
            java.util.Map.of("actions", "cases"));
    }

    @Override
    public void run(Cfg<COND, ACTION> cfg, CTX ctx) {
        List<Case<COND, ACTION>> cases = cfg.cases;
        int size = cases.size();
        boolean[] matched = new boolean[size];
        for (int i = 0; i < size; i++) {
            matched[i] = condTester.test(cases.get(i).condition, ctx);
        }
        for (int i = 0; i < size; i++) {
            if (matched[i]) actionRunner.accept(cases.get(i).action, ctx);
        }
    }
}
