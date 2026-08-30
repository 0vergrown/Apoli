package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.keybind.KeyDispatch;

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
        KeyDispatch.force(ctx.raw(), cfg.key.key(), cfg.duration, cfg.release);
    }
}
