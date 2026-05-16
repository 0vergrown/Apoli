package dev.overgrown.apoli.action;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.resources.ResourceLocation;

public record ItemAction(ResourceLocation typeId, Object config) {
    public ItemAction {
        typeId = ActionTypes.ITEM.resolveId(typeId);
    }

    public void run(ItemCtx ctx) {
        ActionType<ItemCtx, ?> type = ActionTypes.ITEM.get(typeId);
        if (type == null) return;
        @SuppressWarnings({"unchecked", "rawtypes"})
        ActionType raw = type;
        raw.run(config, ctx);
    }

    public static final Codec<ItemAction> CODEC = DispatchedTypeCodec.create(
        "item_action",
        id -> {
            ResourceLocation canonical = ActionTypes.ITEM.resolveId(id);
            ActionType<ItemCtx, ?> t = ActionTypes.ITEM.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        ItemAction::new,
        ItemAction::typeId,
        ItemAction::config
    );
}
