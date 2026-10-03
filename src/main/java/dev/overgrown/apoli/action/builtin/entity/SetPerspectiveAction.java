package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.CameraPerspective;
import dev.overgrown.apoli.entity.CameraPerspectives;
import dev.overgrown.apoli.network.payload.SetPerspectiveS2C;
import net.minecraft.server.level.ServerPlayer;

public final class SetPerspectiveAction implements ActionType<EntityCtx, SetPerspectiveAction.Cfg> {
    public record Cfg(CameraPerspective perspective) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            CameraPerspective.CODEC.fieldOf("perspective").forGetter(Cfg::perspective)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        CameraPerspectives.setType(player.getUUID(), cfg.perspective().id());
        ApoliNetwork.sendPerspective(player, new SetPerspectiveS2C(cfg.perspective().id()));
    }
}
