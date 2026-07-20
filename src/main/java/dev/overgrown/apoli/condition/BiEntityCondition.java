package dev.overgrown.apoli.condition;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.resources.ResourceLocation;

public record BiEntityCondition(ResourceLocation typeId, Object config, boolean inverted) {
    public BiEntityCondition(ResourceLocation typeId, Object config) {
        this(typeId, config, false);
    }

    public BiEntityCondition {
        typeId = ConditionTypes.BI_ENTITY.resolveId(typeId);
    }

    public boolean test(BiEntityCtx ctx) {
        ConditionType<BiEntityCtx, ?> type = ConditionTypes.BI_ENTITY.get(typeId);
        if (type == null) return true;
        if ((ctx.actor() == null || ctx.target() == null) && !type.acceptsNonLiving()) return inverted;
        @SuppressWarnings({"unchecked", "rawtypes"})
        boolean result = ((ConditionType) type).test(config, ctx);
        return inverted != result;
    }

    public static final Codec<BiEntityCondition> CODEC = DispatchedTypeCodec.createInvertible(
        "bi_entity_condition",
        id -> {
            ResourceLocation canonical = ConditionTypes.BI_ENTITY.resolveId(id);
            ConditionType<BiEntityCtx, ?> t = ConditionTypes.BI_ENTITY.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        BiEntityCondition::new,
        BiEntityCondition::typeId,
        BiEntityCondition::config,
        BiEntityCondition::inverted
    );
}
