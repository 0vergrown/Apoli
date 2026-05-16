package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;

public final class InvertBiEntityAction implements ActionType<BiEntityCtx, InvertBiEntityAction.Cfg> {
    public record Cfg(BiEntityAction action) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.fieldOf("action").forGetter(Cfg::action)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        cfg.action.run(ctx.swap());
    }
}