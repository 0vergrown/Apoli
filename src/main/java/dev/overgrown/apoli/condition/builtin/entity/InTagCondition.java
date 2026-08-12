package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class InTagCondition implements ConditionType<EntityCtx, InTagCondition.Cfg> {
    public record Cfg(TagKey<EntityType<?>> tag) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.<EntityType<?>>tagKey(Registries.ENTITY_TYPE).fieldOf("tag").forGetter(Cfg::tag)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        return ctx.raw().getType().is(cfg.tag);
    }
}
