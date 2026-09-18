package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.tick.TickRateVanilla;
import dev.overgrown.apoli.tick.TickRates;
import dev.overgrown.apoli.tick.TickScope;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

public final class TickRateBlockCondition implements ConditionType<BlockCtx, TickRateBlockCondition.Cfg> {

    public record Cfg(TickScope scope, Optional<Comparison> comparison, Optional<Expression> compareTo,
                      Optional<Boolean> frozen) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TickScope.CODEC.optionalFieldOf("scope", TickScope.CHUNK).forGetter(Cfg::scope),
            Comparison.CODEC.optionalFieldOf("comparison").forGetter(Cfg::comparison),
            Expression.INT_OR_EXPR.optionalFieldOf("compare_to").forGetter(Cfg::compareTo),
            Codec.BOOL.optionalFieldOf("frozen").forGetter(Cfg::frozen)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        MinecraftServer server = ctx.level().getServer();
        int base = TickRateVanilla.rate(server);
        int rate = cfg.scope == TickScope.DIMENSION
            ? TickRates.effectiveDimensionRate(ctx.level(), base)
            : TickRates.effectiveChunkRate(ctx.level(), ChunkPos.asLong(ctx.pos()), base);
        if (cfg.frozen.isPresent() && cfg.frozen.get() != (rate == 0)) return false;
        if (cfg.comparison.isEmpty()) return true;
        double compareTo = cfg.compareTo.map(expression -> expression.eval(ctx.actor())).orElse(0.0);
        return cfg.comparison.get().compare(rate, compareTo);
    }
}
