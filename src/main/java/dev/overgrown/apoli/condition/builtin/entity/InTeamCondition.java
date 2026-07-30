package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.TeamData;
import net.minecraft.world.scores.Team;

import java.util.List;
import java.util.Optional;

public final class InTeamCondition implements ConditionType<EntityCtx, InTeamCondition.Cfg> {

    public record Cfg(Optional<TeamData> team, List<TeamData> teams) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TeamData.CODEC.optionalFieldOf("team").forGetter(Cfg::team),
            TeamData.LIST_OR_SINGLE.optionalFieldOf("teams", List.of()).forGetter(Cfg::teams)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Team team = ctx.entity().getTeam();
        if (team == null) return false;
        if (cfg.team.isEmpty() && cfg.teams.isEmpty()) return true;
        if (cfg.team.isPresent() && cfg.team.get().matches(team)) return true;
        for (int i = 0; i < cfg.teams.size(); i++) {
            if (cfg.teams.get(i).matches(team)) return true;
        }
        return false;
    }
}
