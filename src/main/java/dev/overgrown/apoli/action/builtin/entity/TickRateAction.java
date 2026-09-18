package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.tick.TickRateApply;
import dev.overgrown.apoli.tick.TickRates;
import dev.overgrown.apoli.tick.TickScope;
import dev.overgrown.apoli.tick.TickState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class TickRateAction implements ActionType<EntityCtx, TickRateAction.Cfg> {

    public record Cfg(
        TickScope scope,
        Optional<Expression> rate,
        Optional<Boolean> frozen,
        Optional<Expression> step,
        Optional<Expression> sprint,
        boolean reset,
        int duration
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TickScope.CODEC.optionalFieldOf("scope", TickScope.ENTITY).forGetter(Cfg::scope),
            Expression.INT_OR_EXPR.optionalFieldOf("rate").forGetter(Cfg::rate),
            Codec.BOOL.optionalFieldOf("frozen").forGetter(Cfg::frozen),
            Expression.INT_OR_EXPR.optionalFieldOf("step").forGetter(Cfg::step),
            Expression.INT_OR_EXPR.optionalFieldOf("sprint").forGetter(Cfg::sprint),
            Codec.BOOL.optionalFieldOf("reset", false).forGetter(Cfg::reset),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(Cfg::duration)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null || !(ctx.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;

        if (cfg.scope == TickScope.SERVER) {
            TickRateApply.applyServer(server, cfg.rate, cfg.frozen, cfg.step, cfg.sprint, entity, cfg.reset, cfg.duration);
            return;
        }

        if (cfg.reset) {
            TickRateApply.reset(cfg.scope, entity);
            return;
        }

        TickState state = TickRates.stateFor(entity, cfg.scope);
        TickRateApply.apply(state, server, cfg.rate, cfg.frozen, cfg.step, cfg.sprint, entity, cfg.duration);
        TickRates.prune(cfg.scope, entity);
    }
}
