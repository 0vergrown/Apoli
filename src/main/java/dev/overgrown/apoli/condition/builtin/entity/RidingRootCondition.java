package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class RidingRootCondition implements ConditionType<EntityCtx, RidingRootCondition.Cfg> {
    public record Cfg(Optional<BiEntityCondition> bientityCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::bientityCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity actor = ctx.entity();
        Entity root = actor.getRootVehicle();
        if (root == null || root == actor) return false;
        if (cfg.bientityCondition.isEmpty()) return true;
        return cfg.bientityCondition.get().test(new BiEntityCtx(actor, root, ctx.level()));
    }
}
