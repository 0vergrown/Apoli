package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.script.ApoliScripts;
import dev.overgrown.apoli.script.ScriptCtx;
import dev.overgrown.apoli.script.ScriptRef;

public final class ScriptItemCondition implements ConditionType<ItemCtx, ScriptRef> {
    @Override
    public MapCodec<ScriptRef> codec() {
        return ScriptRef.CODEC;
    }

    @Override
    public boolean test(ScriptRef cfg, ItemCtx ctx) {
        return ApoliScripts.test(ApoliScripts.Kind.ITEM_CONDITION, cfg.script(), ScriptCtx.of(ctx, cfg.params()), false);
    }
}
