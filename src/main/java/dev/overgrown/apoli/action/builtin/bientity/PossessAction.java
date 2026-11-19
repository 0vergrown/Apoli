package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.entity.possession.PossessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

import java.util.Optional;

public final class PossessAction implements ActionType<BiEntityCtx, PossessAction.Cfg> {

    public record Cfg(Expression duration, Optional<BiEntityAction> endAction, boolean hostAttacks) {}

    private static final Expression FOREVER = Expression.constant(-1);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.INT_OR_EXPR.optionalFieldOf("duration", FOREVER).forGetter(Cfg::duration),
            LoggedOptionalField.of("end_action", BiEntityAction.CODEC).forGetter(Cfg::endAction),
            Codec.BOOL.optionalFieldOf("host_attacks", true).forGetter(Cfg::hostAttacks)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        if (!(ctx.actor() instanceof ServerPlayer possessor) || !(ctx.target() instanceof Mob host)) return;
        PossessionManager.start(possessor, host, cfg.duration().evalInt(possessor), cfg.endAction().orElse(null),
            cfg.hostAttacks());
    }
}
