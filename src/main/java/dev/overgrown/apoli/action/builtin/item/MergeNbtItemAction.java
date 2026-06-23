package dev.overgrown.apoli.action.builtin.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Nbt;

public final class MergeNbtItemAction implements ActionType<ItemCtx, MergeNbtItemAction.Cfg> {
    public record Cfg(Nbt nbt) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Nbt.CODEC.fieldOf("nbt").forGetter(Cfg::nbt)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, ItemCtx ctx) {
        if (ctx.stack() == null || ctx.stack().isEmpty()) return;
        ctx.stack().getOrCreateTag().merge(cfg.nbt.tag());
    }
}
