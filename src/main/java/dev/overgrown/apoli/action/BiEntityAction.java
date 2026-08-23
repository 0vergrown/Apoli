package dev.overgrown.apoli.action;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.expr.ExprPeer;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;

public record BiEntityAction(ResourceLocation typeId, Object config) {
    public BiEntityAction {
        typeId = ActionTypes.BI_ENTITY.resolveId(typeId);
    }

    public void run(BiEntityCtx ctx) {
        ActionType<BiEntityCtx, ?> type = ActionTypes.BI_ENTITY.get(typeId);
        if (type == null) return;
        if (ctx.actor() == null && ctx.target() == null) return;
        @SuppressWarnings({"unchecked", "rawtypes"})
        ActionType raw = type;
        Entity[] frame = ExprPeer.frame();
        Entity previousActor = frame[ExprPeer.ACTOR];
        Entity previousTarget = frame[ExprPeer.TARGET];
        frame[ExprPeer.ACTOR] = ctx.actor();
        frame[ExprPeer.TARGET] = ctx.target();
        try {
            raw.run(config, ctx);
        } finally {
            frame[ExprPeer.ACTOR] = previousActor;
            frame[ExprPeer.TARGET] = previousTarget;
        }
    }

    public static final Codec<BiEntityAction> CODEC = DispatchedTypeCodec.create(
        "bi_entity_action",
        id -> {
            ResourceLocation canonical = ActionTypes.BI_ENTITY.resolveId(id);
            ActionType<BiEntityCtx, ?> t = ActionTypes.BI_ENTITY.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        BiEntityAction::new,
        BiEntityAction::typeId,
        BiEntityAction::config
    );
}
