package dev.overgrown.apoli.action.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.ItemCtx;

public final class ConsumeAction implements ActionType<ItemCtx, ConsumeAction.Cfg> {
    public record Cfg(int amount) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("amount", 1).forGetter(Cfg::amount)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, ItemCtx ctx) {
        if (cfg.amount <= 0) return;
        ctx.stack().shrink(cfg.amount);
    }
}
