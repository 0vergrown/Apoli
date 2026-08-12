package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.IdOrTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;

public final class EntityTypeCondition implements ConditionType<EntityCtx, EntityTypeCondition.Cfg> {
    public record Cfg(IdOrTag<EntityType<?>> entityType) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdOrTag.<EntityType<?>>codec(Registries.ENTITY_TYPE).fieldOf("entity_type").forGetter(Cfg::entityType)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        return cfg.entityType.matches(ctx.raw().getType().builtInRegistryHolder());
    }
}
