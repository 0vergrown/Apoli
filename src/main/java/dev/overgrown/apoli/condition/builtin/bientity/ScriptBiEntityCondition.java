package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.script.ApoliScripts;
import dev.overgrown.apoli.script.ScriptCtx;
import dev.overgrown.apoli.script.ScriptRef;

public final class ScriptBiEntityCondition implements ConditionType<BiEntityCtx, ScriptRef> {
    @Override
    public MapCodec<ScriptRef> codec() {
        return ScriptRef.CODEC;
    }

    @Override
    public boolean test(ScriptRef cfg, BiEntityCtx ctx) {
        return ApoliScripts.test(ApoliScripts.Kind.BIENTITY_CONDITION, cfg.script(), ScriptCtx.of(ctx, cfg.params()), false);
    }
}
