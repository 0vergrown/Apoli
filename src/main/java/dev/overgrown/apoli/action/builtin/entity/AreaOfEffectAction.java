package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Shape;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AreaOfEffectAction implements ActionType<EntityCtx, AreaOfEffectAction.Cfg> {
    public record Cfg(
        Vector radius,
        Shape shape,
        BiEntityAction bientityAction,
        Optional<BiEntityCondition> bientityCondition,
        boolean includeActor
    ) {}

    private static final MapCodec<Cfg> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
        Vector.SCALAR_OR_VECTOR.optionalFieldOf("radius", Vector.uniform(16.0f)).forGetter(Cfg::radius),
        Shape.CODEC.optionalFieldOf("shape", Shape.CUBE).forGetter(Cfg::shape),
        BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(Cfg::bientityAction),
        BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Cfg::bientityCondition),
        Codec.BOOL.optionalFieldOf("include_actor", false).forGetter(Cfg::includeActor)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(INNER, Map.of("include_target", "include_actor"));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity actor = ctx.raw();
        BlockPos center = actor.blockPosition();
        int rx = (int) Math.ceil(cfg.radius.x());
        int ry = (int) Math.ceil(cfg.radius.y());
        int rz = (int) Math.ceil(cfg.radius.z());
        int boxHalf = Math.max(rx, Math.max(ry, rz));
        AABB box = AABB.ofSize(actor.position(), boxHalf * 2.0, boxHalf * 2.0, boxHalf * 2.0);
        List<LivingEntity> nearby = actor.level().getEntitiesOfClass(LivingEntity.class, box,
            target -> {
                if (target == actor) return cfg.includeActor;
                boolean constrained = cfg.shape != Shape.CUBE || rx != ry || ry != rz;
                if (constrained) {
                    BlockPos tp = target.blockPosition();
                    if (!cfg.shape.contains(tp.getX() - center.getX(), tp.getY() - center.getY(), tp.getZ() - center.getZ(), rx, ry, rz)) return false;
                }
                if (cfg.bientityCondition.isPresent()) {
                    if (!cfg.bientityCondition.get().test(BiEntityCtx.of(actor, target, actor.level()))) return false;
                }
                return true;
            });
        for (LivingEntity target : nearby) {
            cfg.bientityAction.run(BiEntityCtx.of(actor, target, actor.level()));
        }
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
