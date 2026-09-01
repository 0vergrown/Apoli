package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class OwnerCondition implements ConditionType<EntityCtx, OwnerCondition.Cfg> {
    public record Cfg(Optional<BiEntityCondition> bientityCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::bientityCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity owner = ownerOf(ctx.raw());
        if (owner == null) return false;
        if (cfg.bientityCondition.isEmpty()) return true;
        return cfg.bientityCondition.get().test(new BiEntityCtx(ctx.raw(), owner, ctx.level()));
    }

    @Nullable
    private static Entity ownerOf(Entity entity) {
        if (entity instanceof OwnableEntity ownable) return ownable.getOwner();
        if (entity instanceof Projectile projectile) return projectile.getOwner();
        return null;
    }
}
