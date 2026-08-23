package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.script.ApoliScripts;
import dev.overgrown.apoli.script.ScriptCtx;
import dev.overgrown.apoli.script.ScriptRef;

public final class ScriptCondition implements ConditionType<EntityCtx, ScriptRef> {
    @Override
    public MapCodec<ScriptRef> codec() {
        return ScriptRef.CODEC;
    }

    @Override
    public boolean test(ScriptRef cfg, EntityCtx ctx) {
        return ApoliScripts.test(ApoliScripts.Kind.ENTITY_CONDITION, cfg.script(), ScriptCtx.of(ctx, cfg.params()), false);
    }
}
