package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class MountAction implements ActionType<BiEntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity target = ctx.target();
        if (actor == null || target == null) return;
        if (!actor.startRiding(target, true) || target.level().isClientSide()) return;
        ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(target);
        if (target instanceof ServerPlayer player) {
            player.connection.send(packet);
        }
        if (actor instanceof ServerPlayer player) {
            player.connection.send(packet);
        }
    }
}
