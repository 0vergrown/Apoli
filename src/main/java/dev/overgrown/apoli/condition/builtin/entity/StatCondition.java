package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Stat;
import net.minecraft.server.level.ServerPlayer;

public final class StatCondition implements ConditionType<EntityCtx, StatCondition.Cfg> {
    public record Cfg(Stat stat, Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Stat.CODEC.fieldOf("stat").forGetter(Cfg::stat),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.raw() instanceof ServerPlayer player)) return false;
        net.minecraft.stats.Stat<?> stat = cfg.stat.resolve();
        if (stat == null) return false;
        return cfg.comparison.compare(player.getStats().getValue(stat), cfg.compareTo);
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
