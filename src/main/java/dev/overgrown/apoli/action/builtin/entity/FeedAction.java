package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.player.Player;

public final class FeedAction implements ActionType<EntityCtx, FeedAction.Cfg> {
    public record Cfg(int food, float saturation) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("food").forGetter(Cfg::food),
            Codec.FLOAT.fieldOf("saturation").forGetter(Cfg::saturation)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (ctx.entity() instanceof Player player) {
            player.getFoodData().eat(cfg.food, cfg.saturation);
        }
    }
}
