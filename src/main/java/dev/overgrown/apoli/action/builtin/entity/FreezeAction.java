package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;

public final class FreezeAction implements ActionType<EntityCtx, FreezeAction.Cfg> {
    public record Cfg(Expression duration) {
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Expression.INT_OR_EXPR.fieldOf("duration").forGetter(Cfg::duration)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        int frozenDuration = cfg.duration.evalInt(ctx.entity());

        if (frozenDuration < 0) {
            ctx.raw().setTicksFrozen(1000000000);
        } else {
            ctx.raw().setTicksFrozen((cfg.duration.evalInt(ctx.entity())));
        }
    }
}
