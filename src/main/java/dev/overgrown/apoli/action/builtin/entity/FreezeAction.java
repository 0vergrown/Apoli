package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;

public final class FreezeAction implements ActionType<EntityCtx, FreezeAction.Cfg> {
    public static final int PERMANENT = 1_000_000_000;

    private static final int DRAIN_PER_TICK = 2;

    public record Cfg(Optional<Expression> duration) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("duration", Expression.INT_OR_EXPR)
                .forGetter(Cfg::duration)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (cfg.duration.isEmpty()) {
            entity.setTicksFrozen(entity.getTicksRequiredToFreeze() + DRAIN_PER_TICK);
            return;
        }
        int ticks = cfg.duration.get().evalInt(entity);
        entity.setTicksFrozen(ticks < 0 ? PERMANENT : ticks);
    }
}
