package dev.overgrown.apoli.action;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.BlockCtx;
import net.minecraft.resources.ResourceLocation;

public record BlockAction(ResourceLocation typeId, Object config) {
    public BlockAction {
        typeId = ActionTypes.BLOCK.resolveId(typeId);
    }

    public void run(BlockCtx ctx) {
        ActionType<BlockCtx, ?> type = ActionTypes.BLOCK.get(typeId);
        if (type == null) return;
        @SuppressWarnings({"unchecked", "rawtypes"})
        ActionType raw = type;
        raw.run(config, ctx);
    }

    public static final Codec<BlockAction> CODEC = DispatchedTypeCodec.create(
        "block_action",
        id -> {
            ResourceLocation canonical = ActionTypes.BLOCK.resolveId(id);
            ActionType<BlockCtx, ?> t = ActionTypes.BLOCK.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        BlockAction::new,
        BlockAction::typeId,
        BlockAction::config
    );
}
