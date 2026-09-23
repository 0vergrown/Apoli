package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.entity.GrabManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.player.Player;

public final class AggroAtAction implements ActionType<BiEntityCtx, AggroAtAction.Cfg> {

    public record Cfg(Expression duration) {}

    private static final Expression FOREVER = Expression.constant(-1);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Expression.INT_OR_EXPR.optionalFieldOf("duration", FOREVER).forGetter(Cfg::duration)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        if (!(ctx.target() instanceof NeutralMob neutralMob)) return;
        if (!(ctx.actor() instanceof Player player)) return;

        neutralMob.setPersistentAngerTarget(player.getUUID());
        neutralMob.setRemainingPersistentAngerTime((cfg.duration.evalInt(player)));
    }
}