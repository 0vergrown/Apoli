package dev.overgrown.apoli.condition;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.DispatchedTypeCodec;
import dev.overgrown.apoli.condition.context.BiomeCtx;
import net.minecraft.resources.ResourceLocation;

public record BiomeCondition(ResourceLocation typeId, Object config) {
    public BiomeCondition {
        typeId = ConditionTypes.BIOME.resolveId(typeId);
    }

    public boolean test(BiomeCtx ctx) {
        ConditionType<BiomeCtx, ?> type = ConditionTypes.BIOME.get(typeId);
        if (type == null) return true;
        @SuppressWarnings({"unchecked", "rawtypes"})
        boolean result = ((ConditionType) type).test(config, ctx);
        return result;
    }

    public static final Codec<BiomeCondition> CODEC = DispatchedTypeCodec.create(
        "biome_condition",
        id -> {
            ResourceLocation canonical = ConditionTypes.BIOME.resolveId(id);
            ConditionType<BiomeCtx, ?> t = ConditionTypes.BIOME.get(canonical);
            return t == null ? null : new DispatchedTypeCodec.CodecLookup.Resolution(canonical, t.codec());
        },
        BiomeCondition::new,
        BiomeCondition::typeId,
        BiomeCondition::config
    );
}
