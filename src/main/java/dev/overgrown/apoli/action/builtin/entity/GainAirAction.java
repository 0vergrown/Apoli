package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.LivingEntity;

public final class GainAirAction implements ActionType<EntityCtx, GainAirAction.Cfg> {
    public record Cfg(int value) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("value").forGetter(Cfg::value)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity e = ctx.entity();
        e.setAirSupply(Math.min(e.getAirSupply() + cfg.value, e.getMaxAirSupply()));
    }
}
