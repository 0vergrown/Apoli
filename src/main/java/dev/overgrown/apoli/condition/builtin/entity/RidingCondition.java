package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class RidingCondition implements ConditionType<EntityCtx, RidingCondition.Cfg> {
    public record Cfg(Optional<BiEntityCondition> bientityCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Cfg::bientityCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity vehicle = ctx.entity().getVehicle();
        if (!(vehicle instanceof LivingEntity livingVehicle)) return false;
        if (cfg.bientityCondition.isEmpty()) return true;
        return cfg.bientityCondition.get().test(new BiEntityCtx(ctx.entity(), livingVehicle, ctx.level()));
    }
}
