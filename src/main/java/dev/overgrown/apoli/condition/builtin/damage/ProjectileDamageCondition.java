package dev.overgrown.apoli.condition.builtin.damage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class ProjectileDamageCondition implements ConditionType<DamageCtx, ProjectileDamageCondition.Cfg> {
    public record Cfg(Optional<ResourceLocation> projectile, Optional<EntityCondition> projectileCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("projectile").forGetter(Cfg::projectile),
            EntityCondition.CODEC.optionalFieldOf("projectile_condition").forGetter(Cfg::projectileCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, DamageCtx ctx) {
        if (!ctx.source().is(DamageTypeTags.IS_PROJECTILE)) return false;
        Entity projectile = ctx.source().getDirectEntity();
        if (projectile == null) return false;
        if (cfg.projectile.isPresent()) {
            EntityType<?> expected = BuiltInRegistries.ENTITY_TYPE.get(cfg.projectile.get());
            if (expected == null || projectile.getType() != expected) return false;
        }
        if (cfg.projectileCondition.isPresent()) {
            LivingEntity living = projectile instanceof LivingEntity le ? le : null;
            EntityCtx projCtx = new EntityCtx(living, ctx.level(), projectile);
            try {
                return cfg.projectileCondition.get().test(projCtx);
            } catch (RuntimeException e) {
                return false;
            }
        }
        return true;
    }
}
