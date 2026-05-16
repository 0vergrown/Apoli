package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;

public final class SetOnFireAction implements ActionType<EntityCtx, SetOnFireAction.Cfg> {
    public record Cfg(int duration) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("duration").forGetter(Cfg::duration)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        ctx.entity().setSecondsOnFire(cfg.duration);
    }
}