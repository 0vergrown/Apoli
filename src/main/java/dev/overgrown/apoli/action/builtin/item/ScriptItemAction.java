package dev.overgrown.apoli.action.builtin.item;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.script.ApoliScripts;
import dev.overgrown.apoli.script.ScriptCtx;
import dev.overgrown.apoli.script.ScriptRef;

public final class ScriptItemAction implements ActionType<ItemCtx, ScriptRef> {
    @Override
    public MapCodec<ScriptRef> codec() {
        return ScriptRef.CODEC;
    }

    @Override
    public void run(ScriptRef cfg, ItemCtx ctx) {
        if (ctx.level() == null || ctx.level().isClientSide()) return;
        ApoliScripts.run(ApoliScripts.Kind.ITEM_ACTION, cfg.script(), ScriptCtx.of(ctx, cfg.params()));
    }
}
