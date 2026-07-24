package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Hand;
import net.minecraft.world.entity.LivingEntity;

public final class SwingHandAction implements ActionType<EntityCtx, SwingHandAction.Cfg> {
    public record Cfg(Hand hand) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Hand.CODEC.optionalFieldOf("hand", Hand.MAIN_HAND).forGetter(Cfg::hand)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity living = ctx.living();
        if (living != null) living.swing(cfg.hand.vanilla(), true);
    }
}
