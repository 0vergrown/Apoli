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
        @SuppressWarnings({"unchecked", "rawtypes"})
        ActionType raw = type;
        raw.run(config, ctx);
    }

    public static final Codec<EntityAction> CODEC = DispatchedTypeCodec.create(
        "entity_action",
        id -> {
            ResourceLocation canonical = ActionTypes.ENTITY.resolveId(id);
            ActionType<EntityCtx, ?> t = ActionTypes.ENTITY.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        EntityAction::new,
        EntityAction::typeId,
        EntityAction::config
    );
}
