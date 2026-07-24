package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public final class EntityTypeCondition implements ConditionType<EntityCtx, EntityTypeCondition.Cfg> {
    public record Cfg(ResourceLocation entityType) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("entity_type").forGetter(Cfg::entityType)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(cfg.entityType);
        return type != null && ctx.raw().getType().equals(type);
    }
}
