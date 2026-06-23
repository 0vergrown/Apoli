package dev.overgrown.apoli.condition;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.FluidCtx;
import net.minecraft.resources.ResourceLocation;

public record FluidCondition(ResourceLocation typeId, Object config, boolean inverted) {
    public FluidCondition(ResourceLocation typeId, Object config) {
        this(typeId, config, false);
    }

    public FluidCondition {
        typeId = ConditionTypes.FLUID.resolveId(typeId);
    }

    public boolean test(FluidCtx ctx) {
        ConditionType<FluidCtx, ?> type = ConditionTypes.FLUID.get(typeId);
        if (type == null) return true;
        @SuppressWarnings({"unchecked", "rawtypes"})
        boolean result = ((ConditionType) type).test(config, ctx);
        return inverted != result;
    }

    public static final Codec<FluidCondition> CODEC = DispatchedTypeCodec.createInvertible(
        "fluid_condition",
        id -> {
            ResourceLocation canonical = ConditionTypes.FLUID.resolveId(id);
            ConditionType<FluidCtx, ?> t = ConditionTypes.FLUID.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        FluidCondition::new,
        FluidCondition::typeId,
        FluidCondition::config,
        FluidCondition::inverted
    );
}
