package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.network.payload.ForceKeyS2C;
import net.minecraft.server.level.ServerPlayer;

public final class ForceKeyPressedAction implements ActionType<EntityCtx, ForceKeyPressedAction.Cfg> {
    public record Cfg(Key key, int duration, boolean release) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Key.CODEC.optionalFieldOf("key", Key.DEFAULT_PRIMARY).forGetter(Cfg::key),
            Codec.INT.optionalFieldOf("duration", 1).forGetter(Cfg::duration),
            Codec.BOOL.optionalFieldOf("release", false).forGetter(Cfg::release)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.raw() instanceof ServerPlayer player)) return;
        ApoliNetwork.sendForceKey(player, new ForceKeyS2C(cfg.key.key(), Math.max(1, cfg.duration), cfg.release));
    }
}
