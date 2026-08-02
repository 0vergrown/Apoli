package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.PlayerModelType;
import dev.overgrown.apoli.entity.PlayerModelTypes;
import net.minecraft.world.entity.player.Player;

public final class PlayerModelTypeCondition implements ConditionType<EntityCtx, PlayerModelTypeCondition.Cfg> {
    public record Cfg(PlayerModelType modelType) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            PlayerModelType.CODEC.fieldOf("model_type").forGetter(Cfg::modelType)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player)) return false;
        return PlayerModelTypes.of(ctx.entity()) == cfg.modelType;
    }
}
