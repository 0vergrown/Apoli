package dev.overgrown.apoli.action;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;

public record EntityAction(ResourceLocation typeId, Object config) {
    public EntityAction {
        typeId = ActionTypes.ENTITY.resolveId(typeId);
    }

    public void run(EntityCtx ctx) {
        ActionType<EntityCtx, ?> type = ActionTypes.ENTITY.get(typeId);
        if (type == null) return;
        if (ctx.entity() == null) return;
        @SuppressWarnings({"unchecked", "rawtypes"})
        ActionType raw = type;
        raw.run(config, ctx);
    }

    public static final Codec<EntityAction> CODEC = DispatchedTypeCodec.create(
        "entity_action",
        id -> {
            ResourceLocation canonical = ActionTypes.ENTITY.resolveId(id);
            ActionType<EntityCtx, ?> t = ActionTypes.ENTITY.get(canonical);
            if (t == null) return null;
            return new DispatchedTypeCodec.CodecLookup.Resolution(canonical,
                ActionTypes.ENTITY.applyAliasDefaults(id, t.codec()));
        },
        EntityAction::new,
        EntityAction::typeId,
        EntityAction::config
    );
}
