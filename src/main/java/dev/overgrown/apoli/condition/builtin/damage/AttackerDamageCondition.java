package dev.overgrown.apoli.condition.builtin.damage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class AttackerDamageCondition implements ConditionType<DamageCtx, AttackerDamageCondition.Cfg> {
    public record Cfg(Optional<EntityCondition> entityCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("entity_condition", EntityCondition.CODEC).forGetter(Cfg::entityCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, DamageCtx ctx) {
        Entity attacker = ctx.source().getEntity();
        if (!(attacker instanceof LivingEntity living)) return false;
        if (cfg.entityCondition.isEmpty()) return true;
        return cfg.entityCondition.get().test(new EntityCtx(living, ctx.level()));
    }
}
