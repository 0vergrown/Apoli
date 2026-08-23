package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.script.ApoliScripts;
import dev.overgrown.apoli.script.ScriptCtx;
import dev.overgrown.apoli.script.ScriptRef;

public final class ScriptBlockAction implements ActionType<BlockCtx, ScriptRef> {
    @Override
    public MapCodec<ScriptRef> codec() {
        return ScriptRef.CODEC;
    }

    @Override
    public void run(ScriptRef cfg, BlockCtx ctx) {
        if (ctx.level() == null || ctx.level().isClientSide()) return;
        ApoliScripts.run(ApoliScripts.Kind.BLOCK_ACTION, cfg.script(), ScriptCtx.of(ctx, cfg.params()));
    }
}
