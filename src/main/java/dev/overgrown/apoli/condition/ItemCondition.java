package dev.overgrown.apoli.condition;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.resources.ResourceLocation;

public record ItemCondition(ResourceLocation typeId, Object config, boolean inverted) {
    public ItemCondition(ResourceLocation typeId, Object config) {
        this(typeId, config, false);
    }

    public ItemCondition {
        typeId = ConditionTypes.ITEM.resolveId(typeId);
    }

    public boolean test(ItemCtx ctx) {
        ConditionType<ItemCtx, ?> type = ConditionTypes.ITEM.get(typeId);
        if (type == null) return true;
        @SuppressWarnings({"unchecked", "rawtypes"})
        boolean result = ((ConditionType) type).test(config, ctx);
        return inverted != result;
    }

    public static final Codec<ItemCondition> CODEC = DispatchedTypeCodec.createInvertible(
        "item_condition",
        id -> {
            ResourceLocation canonical = ConditionTypes.ITEM.resolveId(id);
            ConditionType<ItemCtx, ?> t = ConditionTypes.ITEM.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        ItemCondition::new,
        ItemCondition::typeId,
        ItemCondition::config,
        ItemCondition::inverted
    );
}
