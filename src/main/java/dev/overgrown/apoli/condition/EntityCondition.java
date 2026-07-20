package dev.overgrown.apoli.condition;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;

public record EntityCondition(ResourceLocation typeId, Object config, boolean inverted) {
    public EntityCondition(ResourceLocation typeId, Object config) {
        this(typeId, config, false);
    }

    public EntityCondition {
        typeId = ConditionTypes.ENTITY.resolveId(typeId);
    }

    public boolean test(EntityCtx ctx) {
        ConditionType<EntityCtx, ?> type = ConditionTypes.ENTITY.get(typeId);
        if (type == null) return true;
        if (ctx.entity() == null && (ctx.raw() == null || !type.acceptsNonLiving())) return inverted;
        @SuppressWarnings({"unchecked", "rawtypes"})
        boolean result = ((ConditionType) type).test(config, ctx);
        return inverted != result;
    }

    public static final Codec<EntityCondition> CODEC = DispatchedTypeCodec.createInvertible(
        "entity_condition",
        id -> {
            ResourceLocation canonical = ConditionTypes.ENTITY.resolveId(id);
            ConditionType<EntityCtx, ?> t = ConditionTypes.ENTITY.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        EntityCondition::new,
        EntityCondition::typeId,
        EntityCondition::config,
        EntityCondition::inverted
    );
}
