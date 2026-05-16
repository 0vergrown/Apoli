package dev.overgrown.apoli.condition;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.resources.ResourceLocation;

public record BiEntityCondition(ResourceLocation typeId, Object config) {
    public BiEntityCondition {
        typeId = ConditionTypes.BI_ENTITY.resolveId(typeId);
    }

    public boolean test(BiEntityCtx ctx) {
        ConditionType<BiEntityCtx, ?> type = ConditionTypes.BI_ENTITY.get(typeId);
        if (type == null) return true;
        @SuppressWarnings({"unchecked", "rawtypes"})
        boolean result = ((ConditionType) type).test(config, ctx);
        return result;
    }

    public static final Codec<BiEntityCondition> CODEC = DispatchedTypeCodec.create(
        "bi_entity_condition",
        id -> {
            ResourceLocation canonical = ConditionTypes.BI_ENTITY.resolveId(id);
            ConditionType<BiEntityCtx, ?> t = ConditionTypes.BI_ENTITY.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        BiEntityCondition::new,
        BiEntityCondition::typeId,
        BiEntityCondition::config
    );
}
