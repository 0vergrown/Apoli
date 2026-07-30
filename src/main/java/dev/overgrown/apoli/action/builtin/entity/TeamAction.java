package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.TeamData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.List;
import java.util.Optional;

public final class TeamAction implements ActionType<EntityCtx, TeamAction.Cfg> {

    public enum Operation implements StringRepresentable {
        JOIN("join"),
        LEAVE("leave"),
        CREATE("create"),
        MODIFY("modify"),
        DELETE("delete"),
        EMPTY("empty");

        private final String id;

        Operation(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public record Cfg(Operation operation, Optional<TeamData> team, boolean createIfMissing) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            StringRepresentable.fromEnum(Operation::values).optionalFieldOf("operation", Operation.JOIN).forGetter(Cfg::operation),
            TeamData.CODEC.optionalFieldOf("team").forGetter(Cfg::team),
            Codec.BOOL.optionalFieldOf("create_if_missing", true).forGetter(Cfg::createIfMissing)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        MinecraftServer server = ctx.level().getServer();
        if (server == null) return;
        Entity entity = ctx.entity();
        Scoreboard scoreboard = server.getScoreboard();

        switch (cfg.operation) {
            case LEAVE -> scoreboard.removePlayerFromTeam(entity.getScoreboardName());
            case JOIN -> {
                PlayerTeam team = resolve(scoreboard, cfg, entity);
                if (team == null) return;
                cfg.team.ifPresent(data -> data.applyTo(team));
                scoreboard.addPlayerToTeam(entity.getScoreboardName(), team);
            }
            case CREATE -> {
                PlayerTeam team = resolve(scoreboard, cfg, entity);
                if (team != null) cfg.team.ifPresent(data -> data.applyTo(team));
            }
            case MODIFY -> {
                PlayerTeam team = target(scoreboard, cfg, entity);
                if (team != null) cfg.team.ifPresent(data -> data.applyTo(team));
            }
            case DELETE -> {
                PlayerTeam team = target(scoreboard, cfg, entity);
                if (team != null) scoreboard.removePlayerTeam(team);
            }
            case EMPTY -> {
                PlayerTeam team = target(scoreboard, cfg, entity);
                if (team == null) return;
                for (String member : List.copyOf(team.getPlayers())) {
                    scoreboard.removePlayerFromTeam(member, team);
                }
            }
        }
    }

    private static PlayerTeam resolve(Scoreboard scoreboard, Cfg cfg, Entity entity) {
        String name = teamName(cfg, entity);
        if (name == null) return null;
        PlayerTeam existing = scoreboard.getPlayerTeam(name);
        if (existing != null) return existing;
        return cfg.createIfMissing ? scoreboard.addPlayerTeam(name) : null;
    }

    private static PlayerTeam target(Scoreboard scoreboard, Cfg cfg, Entity entity) {
        String name = teamName(cfg, entity);
        return name == null ? null : scoreboard.getPlayerTeam(name);
    }

    private static String teamName(Cfg cfg, Entity entity) {
        if (cfg.team.isPresent() && cfg.team.get().name().isPresent()) return cfg.team.get().name().get();
        Team current = entity.getTeam();
        return current == null ? null : current.getName();
    }
}
