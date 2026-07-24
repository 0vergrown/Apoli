package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.world.entity.Entity;

public final class GainAirAction implements ActionType<EntityCtx, GainAirAction.Cfg> {
    public record Cfg(Expression value) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.INT_OR_EXPR.fieldOf("value").forGetter(Cfg::value)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity e = ctx.entity();
        e.setAirSupply(Math.min(e.getAirSupply() + cfg.value.evalInt(e), e.getMaxAirSupply()));
    }
}
