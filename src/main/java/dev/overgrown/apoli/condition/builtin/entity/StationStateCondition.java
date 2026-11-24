package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.builtin.StationPower;
import dev.overgrown.apoli.station.StationCheck;
import dev.overgrown.apoli.station.StationContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class StationStateCondition implements ConditionType<EntityCtx, StationStateCondition.Cfg> {
    public record Cfg(ResourceLocation power, StationCheck state) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("power").forGetter(Cfg::power),
            StationCheck.CODEC.optionalFieldOf("state", StationCheck.LIT).forGetter(Cfg::state)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.living();
        if (entity == null) return false;
        StationContainer station = StationPower.find(entity, cfg.power);
        return station != null && station.check(cfg.state);
    }
}
