package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.player.Player;

public final class ExhaustAction implements ActionType<EntityCtx, ExhaustAction.Cfg> {
    public record Cfg(float amount) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("amount").forGetter(Cfg::amount)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (ctx.entity() instanceof Player p) p.causeFoodExhaustion(cfg.amount);
    }
}