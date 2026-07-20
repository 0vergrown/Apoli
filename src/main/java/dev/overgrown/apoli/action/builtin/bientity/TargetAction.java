package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;

public final class TargetAction implements ActionType<BiEntityCtx, TargetAction.Cfg> {
    public record Cfg(EntityAction action) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.fieldOf("action").forGetter(Cfg::action)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        cfg.action.run(ctx.asTarget());
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
