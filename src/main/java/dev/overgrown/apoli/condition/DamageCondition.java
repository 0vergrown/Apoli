package dev.overgrown.apoli.condition;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.DamageCtx;
import net.minecraft.resources.ResourceLocation;

public record DamageCondition(ResourceLocation typeId, Object config) {
    public DamageCondition {
        typeId = ConditionTypes.DAMAGE.resolveId(typeId);
    }

    public boolean test(DamageCtx ctx) {
        ConditionType<DamageCtx, ?> type = ConditionTypes.DAMAGE.get(typeId);
        if (type == null) return true;
        @SuppressWarnings({"unchecked", "rawtypes"})
        boolean result = ((ConditionType) type).test(config, ctx);
        return result;
    }

    public static final Codec<DamageCondition> CODEC = DispatchedTypeCodec.create(
        "damage_condition",
        id -> {
            ResourceLocation canonical = ConditionTypes.DAMAGE.resolveId(id);
            ConditionType<DamageCtx, ?> t = ConditionTypes.DAMAGE.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        DamageCondition::new,
        DamageCondition::typeId,
        DamageCondition::config
    );
}
