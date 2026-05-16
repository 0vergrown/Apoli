package dev.overgrown.apoli.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record Power(
    ResourceLocation typeId,
    Optional<Component> name,
    Optional<Component> description,
    Optional<EntityCondition> condition,
    Object config
) {
    public Power {
        typeId = PowerTypeRegistry.resolveId(typeId);
    }

    public PowerType<?> type() {
        return PowerTypeRegistry.get(typeId);
    }

    // 1.21.1 partialDispatch returns DataResult<? extends MapCodec<? extends E>> — no .codec() unwrap.
    public static final Codec<Power> CODEC = ResourceLocation.CODEC.partialDispatch(
        "type",
        (Power p) -> DataResult.success(p.typeId()),
        id -> {
            ResourceLocation canonical = PowerTypeRegistry.resolveId(id);
            PowerType<?> type = PowerTypeRegistry.get(canonical);
            if (type == null) return DataResult.<MapCodec<? extends Power>>error(() -> "Unknown power type: " + id);
            return DataResult.<MapCodec<? extends Power>>success(buildMapCodec(canonical, type));
        }
    );

    private static <C> MapCodec<Power> buildMapCodec(ResourceLocation typeId, PowerType<C> type) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(Power::name),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(Power::description),
            EntityCondition.CODEC.optionalFieldOf("condition").forGetter(Power::condition),
            type.configCodec().forGetter((Power p) -> {
                @SuppressWarnings("unchecked")
                C cfg = (C) p.config();
                return cfg;
            })
        ).apply(instance, (name, desc, cond, cfg) -> new Power(typeId, name, desc, cond, cfg)));
    }
}
