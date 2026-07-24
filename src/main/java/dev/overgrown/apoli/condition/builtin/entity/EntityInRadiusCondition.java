package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Shape;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

public final class EntityInRadiusCondition implements ConditionType<EntityCtx, EntityInRadiusCondition.Cfg> {
    public record Cfg(Optional<BiEntityCondition> bientityCondition, float radius, Shape shape,
                      Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Cfg::bientityCondition),
            Codec.FLOAT.fieldOf("radius").forGetter(Cfg::radius),
            Shape.CODEC.optionalFieldOf("shape", Shape.CUBE).forGetter(Cfg::shape),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison),
            Codec.INT.optionalFieldOf("compare_to", 1).forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity self = ctx.entity();

        double r = cfg.radius;
        AABB box = new AABB(self.getX() - r, self.getY() - r, self.getZ() - r,
                            self.getX() + r, self.getY() + r, self.getZ() + r);
        List<Entity> nearby = ctx.level().getEntities(self, box);
        boolean filtered = cfg.bientityCondition.isPresent();

        int count = 0;
        for (Entity candidate : nearby) {
            if (!cfg.shape.contains(candidate.getX() - self.getX(), candidate.getY() - self.getY(),
                    candidate.getZ() - self.getZ(), r, r, r)) continue;
            if (filtered) {
                if (!cfg.bientityCondition.get().test(new BiEntityCtx(self, candidate, ctx.level()))) continue;
            }
            count++;
            if (count > cfg.compareTo) break;
        }
        return cfg.comparison.compare(count, cfg.compareTo);
    }
}
