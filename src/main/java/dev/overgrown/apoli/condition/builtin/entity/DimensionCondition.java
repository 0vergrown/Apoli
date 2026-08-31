package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

public final class DimensionCondition implements ConditionType<EntityCtx, DimensionCondition.Cfg> {
    public record Cfg(ResourceLocation dimension) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("dimension").forGetter(Cfg::dimension)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        net.minecraft.world.entity.Entity entity = ctx.entity();
        net.minecraft.world.level.Level level = entity != null ? entity.level() : ctx.level();
        return level != null && level.dimension().location().equals(cfg.dimension);
    }
}
