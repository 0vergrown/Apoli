package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class RidingRecursiveCondition implements ConditionType<EntityCtx, RidingRecursiveCondition.Cfg> {
    public record Cfg(Optional<BiEntityCondition> bientityCondition, Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Cfg::bientityCondition),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison),
            Codec.INT.optionalFieldOf("compare_to", 1).forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity actor = ctx.entity();
        int count = 0;
        Entity vehicle = actor.getVehicle();
        while (vehicle != null) {
            if (cfg.bientityCondition.isEmpty()
                || cfg.bientityCondition.get().test(new BiEntityCtx(actor, vehicle, ctx.level()))) {
                count++;
            }
            vehicle = vehicle.getVehicle();
        }
        return cfg.comparison.compare(count, cfg.compareTo);
    }
}
