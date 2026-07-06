package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.entity.disguise.DisguiseManager;

public final class DisguiseBiEntityAction implements ActionType<BiEntityCtx, DisguiseBiEntityAction.Cfg> {
    public record Cfg(boolean overwrite) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("overwrite", true).forGetter(Cfg::overwrite)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        if (ctx.actor() == null || ctx.target() == null) return;
        DisguiseManager.applyAs(ctx.actor(), ctx.target(), cfg.overwrite);
    }
}
