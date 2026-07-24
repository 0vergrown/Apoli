package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class UsingItemCondition implements ConditionType<EntityCtx, UsingItemCondition.Cfg> {
    public record Cfg(Optional<ItemCondition> itemCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Cfg::itemCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity e = ctx.living();
        if (e == null || !e.isUsingItem()) return false;
        if (cfg.itemCondition.isEmpty()) return true;
        return cfg.itemCondition.get().test(new ItemCtx(e.getUseItem(), ctx.level(), e));
    }
}
