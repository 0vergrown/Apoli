package dev.overgrown.apoli.condition;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.BlockCtx;
import net.minecraft.resources.ResourceLocation;

public record BlockCondition(ResourceLocation typeId, Object config) {
    public BlockCondition {
        typeId = ConditionTypes.BLOCK.resolveId(typeId);
    }

    public boolean test(BlockCtx ctx) {
        ConditionType<BlockCtx, ?> type = ConditionTypes.BLOCK.get(typeId);
        if (type == null) return true;
        @SuppressWarnings({"unchecked", "rawtypes"})
        boolean result = ((ConditionType) type).test(config, ctx);
        return result;
    }

    public static final Codec<BlockCondition> CODEC = DispatchedTypeCodec.create(
        "block_condition",
        id -> {
            ResourceLocation canonical = ConditionTypes.BLOCK.resolveId(id);
            ConditionType<BlockCtx, ?> t = ConditionTypes.BLOCK.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        BlockCondition::new,
        BlockCondition::typeId,
        BlockCondition::config
    );
}
