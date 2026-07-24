package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class DismountAction implements ActionType<EntityCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null) return;
        Entity vehicle = entity.getVehicle();
        entity.stopRiding();
        if (vehicle == null || entity.level().isClientSide()) return;
        ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(vehicle);
        if (vehicle instanceof ServerPlayer player) {
            player.connection.send(packet);
        }
        if (entity instanceof ServerPlayer player) {
            player.connection.send(packet);
        }
    }
}
