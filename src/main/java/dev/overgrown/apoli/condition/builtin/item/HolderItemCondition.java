package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

public final class HolderItemCondition implements ConditionType<ItemCtx, HolderItemCondition.Cfg> {
    public record Cfg(EntityCondition entityCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(RecordCodecBuilder.mapCodec(i -> i.group(
            EntityCondition.CODEC.fieldOf("entity_condition").forGetter(Cfg::entityCondition)
        ).apply(i, Cfg::new)), Map.of("condition", "entity_condition"));
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        LivingEntity holder = ctx.holder();
        return holder != null && cfg.entityCondition.test(EntityCtx.of(holder, ctx.level()));
    }
}
