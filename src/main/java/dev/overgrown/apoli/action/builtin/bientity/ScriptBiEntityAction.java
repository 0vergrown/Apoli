package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.script.ApoliScripts;
import dev.overgrown.apoli.script.ScriptCtx;
import dev.overgrown.apoli.script.ScriptRef;

public final class ScriptBiEntityAction implements ActionType<BiEntityCtx, ScriptRef> {
    @Override
    public MapCodec<ScriptRef> codec() {
        return ScriptRef.CODEC;
    }

    @Override
    public void run(ScriptRef cfg, BiEntityCtx ctx) {
        if (ctx.level() == null || ctx.level().isClientSide()) return;
        ApoliScripts.run(ApoliScripts.Kind.BIENTITY_ACTION, cfg.script(), ScriptCtx.of(ctx, cfg.params()));
    }
}
