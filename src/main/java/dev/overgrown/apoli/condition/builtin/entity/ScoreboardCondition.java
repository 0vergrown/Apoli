package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

import java.util.Optional;

public final class ScoreboardCondition implements ConditionType<EntityCtx, ScoreboardCondition.Cfg> {
    public record Cfg(Optional<String> name, String objective, Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("name").forGetter(Cfg::name),
            Codec.STRING.fieldOf("objective").forGetter(Cfg::objective),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        MinecraftServer server = ctx.level().getServer();
        if (server == null) return false;
        Scoreboard sb = server.getScoreboard();
        Objective obj = sb.getObjective(cfg.objective);
        if (obj == null) return false;
        String holder = cfg.name.orElse(ctx.entity().getScoreboardName());
        if (!sb.hasPlayerScore(holder, obj)) return false;
        int score = sb.getOrCreatePlayerScore(holder, obj).getScore();
        return cfg.comparison.compare(score, cfg.compareTo);
    }
}
