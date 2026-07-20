package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class AddXpAction implements ActionType<EntityCtx, AddXpAction.Cfg> {
    public record Cfg(Optional<Expression> points, Optional<Expression> levels) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.INT_OR_EXPR.optionalFieldOf("points").forGetter(Cfg::points),
            Expression.INT_OR_EXPR.optionalFieldOf("levels").forGetter(Cfg::levels)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return;
        cfg.points.ifPresent(expr -> {
            int p = expr.evalInt(player);
            if (p > 0) player.giveExperiencePoints(p);
        });
        cfg.levels.ifPresent(expr -> player.giveExperienceLevels(expr.evalInt(player)));
    }
}
