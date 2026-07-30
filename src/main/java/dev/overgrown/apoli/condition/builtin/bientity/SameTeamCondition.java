package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Team;

public final class SameTeamCondition implements ConditionType<BiEntityCtx, SameTeamCondition.Cfg> {

    public record Cfg(boolean allowTeamless) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("allow_teamless", false).forGetter(Cfg::allowTeamless)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity target = ctx.target();
        if (actor == null || target == null) return false;
        Team actorTeam = actor.getTeam();
        Team targetTeam = target.getTeam();
        if (actorTeam == null || targetTeam == null) return cfg.allowTeamless && actorTeam == targetTeam;
        return actorTeam.isAlliedTo(targetTeam);
    }
}
