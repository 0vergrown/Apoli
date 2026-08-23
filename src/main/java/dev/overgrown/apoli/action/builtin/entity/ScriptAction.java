package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.script.ApoliScripts;
import dev.overgrown.apoli.script.ScriptCtx;
import dev.overgrown.apoli.script.ScriptRef;

public final class ScriptAction implements ActionType<EntityCtx, ScriptRef> {
    @Override
    public MapCodec<ScriptRef> codec() {
        return ScriptRef.CODEC;
    }

    @Override
    public void run(ScriptRef cfg, EntityCtx ctx) {
        if (ctx.level() == null || ctx.level().isClientSide()) return;
        ApoliScripts.run(ApoliScripts.Kind.ENTITY_ACTION, cfg.script(), ScriptCtx.of(ctx, cfg.params()));
    }
}
