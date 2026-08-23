package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.mount.MountOffsets;
import dev.overgrown.apoli.network.payload.MountOffsetS2C;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class MountAction implements ActionType<BiEntityCtx, MountAction.Cfg> {
    public record Cfg(Expression x, Expression y, Expression z, Space space, boolean force) {}

    private static final Expression ZERO = Expression.constant(0.0);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("x", ZERO).forGetter(Cfg::x),
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("y", ZERO).forGetter(Cfg::y),
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("z", ZERO).forGetter(Cfg::z),
            Space.CODEC.optionalFieldOf("space", Space.WORLD).forGetter(Cfg::space),
            Codec.BOOL.optionalFieldOf("force", true).forGetter(Cfg::force)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity target = ctx.target();
        if (actor == null || target == null || actor == target) return;
        if (actor.getVehicle() != target && !actor.startRiding(target, cfg.force)) return;
        if (target.level().isClientSide()) return;

        MountOffsets.Offset offset = new MountOffsets.Offset(
            cfg.x.eval(actor), cfg.y.eval(actor), cfg.z.eval(actor), cfg.space);
        MountOffsets.put(actor, offset);

        ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(target);
        if (target instanceof ServerPlayer player) {
            player.connection.send(packet);
        }
        if (actor instanceof ServerPlayer player) {
            player.connection.send(packet);
        }
        ApoliNetwork.broadcastMountOffset(actor, new MountOffsetS2C(
            actor.getId(), offset.x(), offset.y(), offset.z(), offset.space()));
    }
}
