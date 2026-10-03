package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.CameraPerspective;
import dev.overgrown.apoli.entity.CameraPerspectives;
import net.minecraft.world.entity.player.Player;

public final class PerspectiveCondition implements ConditionType<EntityCtx, PerspectiveCondition.Cfg> {
    public record Cfg(int perspective) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            CameraPerspective.MASK_CODEC.fieldOf("perspective").forGetter(Cfg::perspective)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        return ctx.entity() instanceof Player player && CameraPerspectives.typeOf(player).matches(cfg.perspective);
    }
}
