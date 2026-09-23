package dev.overgrown.apoli.action.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.server.level.ServerPlayer;

public final class CooldownItemAction implements ActionType<ItemCtx, CooldownItemAction.Cfg> {
    public record Cfg(Expression ticks) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Expression.INT_OR_EXPR.fieldOf("ticks").forGetter(Cfg::ticks)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, ItemCtx ctx) {
        ServerPlayer player = (ServerPlayer) ctx.holder();

        if ((cfg.ticks.evalInt(player)) <= 0) return;
        if (player == null) return;

        player.getCooldowns().addCooldown(ctx.stack().getItem(), (cfg.ticks.evalInt(player)));
    }
}