package dev.overgrown.apoli.condition;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.StaticCtx;
import net.minecraft.resources.ResourceLocation;

public record StaticCondition(ResourceLocation typeId, Object config, boolean inverted) {
    public StaticCondition(ResourceLocation typeId, Object config) {
        this(typeId, config, false);
    }

    public StaticCondition {
        typeId = ConditionTypes.STATIC.resolveId(typeId);
    }

    public boolean test() {
        return test(StaticCtx.INSTANCE);
    }

    public boolean test(StaticCtx ctx) {
        ConditionType<StaticCtx, ?> type = ConditionTypes.STATIC.get(typeId);
        if (type == null) return true;
        @SuppressWarnings({"unchecked", "rawtypes"})
        boolean result = ((ConditionType) type).test(config, ctx);
        return inverted != result;
    }

    public static final Codec<StaticCondition> CODEC = DispatchedTypeCodec.createInvertible(
        "static_condition",
        id -> {
            ResourceLocation canonical = ConditionTypes.STATIC.resolveId(id);
            ConditionType<StaticCtx, ?> t = ConditionTypes.STATIC.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        StaticCondition::new,
        StaticCondition::typeId,
        StaticCondition::config,
        StaticCondition::inverted
    );
}
