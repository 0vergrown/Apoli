package dev.overgrown.apoli.action.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.tick.TickRateApply;
import dev.overgrown.apoli.tick.TickRates;
import dev.overgrown.apoli.tick.TickScope;
import dev.overgrown.apoli.tick.TickState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

public final class TickRateBlockAction implements ActionType<BlockCtx, TickRateBlockAction.Cfg> {

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
            TickScope.CODEC.optionalFieldOf("scope", TickScope.CHUNK).forGetter(Cfg::scope),
            Expression.INT_OR_EXPR.optionalFieldOf("rate").forGetter(Cfg::rate),
            Codec.BOOL.optionalFieldOf("frozen").forGetter(Cfg::frozen),
            Expression.INT_OR_EXPR.optionalFieldOf("step").forGetter(Cfg::step),
            Expression.INT_OR_EXPR.optionalFieldOf("sprint").forGetter(Cfg::sprint),
            Codec.BOOL.optionalFieldOf("reset", false).forGetter(Cfg::reset),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(Cfg::duration)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BlockCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        long chunkPos = ChunkPos.asLong(ctx.pos());

        if (cfg.scope == TickScope.SERVER) {
            dev.overgrown.apoli.tick.TickRateApply.applyServer(server, cfg.rate, cfg.frozen, cfg.step, cfg.sprint,
                ctx.actor(), cfg.reset, cfg.duration);
            return;
        }

        if (cfg.reset) {
            if (cfg.scope == TickScope.DIMENSION) TickRates.clearDimension(level.dimension());
            else TickRateApply.resetChunk(level.dimension(), chunkPos);
            return;
        }

        TickState state = cfg.scope == TickScope.DIMENSION
            ? TickRates.dimensionState(level.dimension())
            : TickRates.chunkState(level.dimension(), chunkPos);
        TickRateApply.apply(state, server, cfg.rate, cfg.frozen, cfg.step, cfg.sprint, ctx.actor(), cfg.duration);
        if (state.rate() < 0 && !state.frozen() && state.stepTicks() == 0 && state.sprintTicks() == 0) {
            if (cfg.scope == TickScope.DIMENSION) TickRates.clearDimension(level.dimension());
            else TickRateApply.resetChunk(level.dimension(), chunkPos);
        }
    }
}
