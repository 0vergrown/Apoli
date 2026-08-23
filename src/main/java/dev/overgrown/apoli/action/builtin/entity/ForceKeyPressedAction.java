package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.keybind.HeldKeys;
import dev.overgrown.apoli.keybind.KeyDispatch;
import dev.overgrown.apoli.network.payload.ForceKeyS2C;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

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
        Entity entity = ctx.raw();
        if (entity == null || !(ctx.level() instanceof ServerLevel)) return;

        String key = cfg.key.key();
        int duration = Math.max(1, cfg.duration);

        if (entity instanceof ServerPlayer player) {
            ApoliNetwork.sendForceKey(player, new ForceKeyS2C(key, duration, cfg.release));
        }

        if (cfg.release) {
            HeldKeys.release(entity.getUUID(), key);
            return;
        }

        HeldKeys.force(entity.getUUID(), key, duration);
        if (!(entity instanceof ServerPlayer)) {
            KeyDispatch.press(entity, key);
        }
    }
}
