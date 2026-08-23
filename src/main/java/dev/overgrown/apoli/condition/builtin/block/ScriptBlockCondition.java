package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.script.ApoliScripts;
import dev.overgrown.apoli.script.ScriptCtx;
import dev.overgrown.apoli.script.ScriptRef;

public final class ScriptBlockCondition implements ConditionType<BlockCtx, ScriptRef> {
    @Override
    public MapCodec<ScriptRef> codec() {
        return ScriptRef.CODEC;
    }

    @Override
    public boolean test(ScriptRef cfg, BlockCtx ctx) {
        return ApoliScripts.test(ApoliScripts.Kind.BLOCK_CONDITION, cfg.script(), ScriptCtx.of(ctx, cfg.params()), false);
    }
}
