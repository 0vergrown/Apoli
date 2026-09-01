package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.entity.SavedLocations;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class HasLocationCondition implements ConditionType<EntityCtx, HasLocationCondition.Cfg> {
    public record Cfg(Optional<String> id) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("id").forGetter(Cfg::id)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null || entity.level().isClientSide()) return false;
        SavedLocations locations = SavedLocations.of(entity);
        if (locations == null) return false;
        return cfg.id.isPresent()
            ? locations.has(entity.getUUID(), cfg.id.get())
            : !locations.ids(entity.getUUID()).isEmpty();
    }
}
