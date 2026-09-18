package dev.overgrown.apoli.tick;

import dev.overgrown.apoli.data.Expression;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class TickRateApply {

    private TickRateApply() {}

    public static void apply(TickState state, MinecraftServer server,
                             Optional<Expression> rate, Optional<Boolean> frozen,
                             Optional<Expression> step, Optional<Expression> sprint,
                             Entity actor, int duration) {
        if (rate.isPresent()) {
            state.setRate(Math.max(0, rate.get().evalInt(actor)));
        }
        frozen.ifPresent(state::setFrozen);
        if (step.isPresent()) {
            state.setStepTicks(Math.max(0, step.get().evalInt(actor)));
        }
        if (sprint.isPresent()) {
            state.setSprintTicks(Math.max(0, sprint.get().evalInt(actor)));
        }
        state.setExpiry(duration <= 0 ? TickState.PERMANENT : server.getTickCount() + duration);
    }

    public static void applyServer(MinecraftServer server,
                                   Optional<Expression> rate, Optional<Boolean> frozen,
                                   Optional<Expression> step, Optional<Expression> sprint,
                                   Entity actor, boolean reset, int duration) {
        if (reset) {
            TickRates.releaseServer(server);
            TickRateVanilla.sprint(server, 0);
            TickRateVanilla.step(server, 0);
            return;
        }
        if (rate.isPresent() || frozen.isPresent()) {
            TickRates.requestServer(server,
                rate.map(expression -> (float) expression.eval(actor)).orElse(TickRateVanilla.exactRate(server)),
                frozen.orElse(TickRateVanilla.frozen(server)),
                duration);
        }
        step.ifPresent(expression -> TickRateVanilla.step(server, expression.evalInt(actor)));
        sprint.ifPresent(expression -> TickRateVanilla.sprint(server, expression.evalInt(actor)));
    }

    public static void reset(TickScope scope, Entity entity) {
        switch (scope) {
            case ENTITY -> TickRates.clearEntity(entity);
            case CHUNK -> TickRates.clearChunk(entity.level().dimension(), entity.chunkPosition().toLong());
            case DIMENSION -> TickRates.clearDimension(entity.level().dimension());
            case SERVER -> {
                MinecraftServer server = entity.getServer();
                if (server != null) TickRates.releaseServer(server);
            }
        }
    }

    public static void resetChunk(ResourceKey<Level> dimension, long chunkPos) {
        TickRates.clearChunk(dimension, chunkPos);
    }
}
