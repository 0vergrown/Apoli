package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancementCondition implements ConditionType<EntityCtx, AdvancementCondition.Cfg> {
    public record Cfg(ResourceLocation advancement) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("advancement").forGetter(Cfg::advancement)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return false;
        AdvancementHolder holder = player.server.getAdvancements().get(cfg.advancement);
        if (holder == null) return false;
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
