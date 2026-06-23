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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AreaOfEffectAction implements ActionType<EntityCtx, AreaOfEffectAction.Cfg> {
    public record Cfg(
        float radius,
        Shape shape,
        BiEntityAction bientityAction,
        Optional<BiEntityCondition> bientityCondition,
        boolean includeActor
    ) {}

    private static final MapCodec<Cfg> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.FLOAT.optionalFieldOf("radius", 16.0f).forGetter(Cfg::radius),
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
        LivingEntity actor = ctx.entity();
        BlockPos center = actor.blockPosition();
        AABB box = AABB.ofSize(actor.position(), cfg.radius * 2, cfg.radius * 2, cfg.radius * 2);
        List<LivingEntity> nearby = actor.level().getEntitiesOfClass(LivingEntity.class, box,
            target -> {
                if (target == actor) return cfg.includeActor;
                if (cfg.shape != Shape.CUBE) {
                    BlockPos tp = target.blockPosition();
                    int r = (int) Math.ceil(cfg.radius);
                    if (!cfg.shape.contains(tp.getX() - center.getX(), tp.getY() - center.getY(), tp.getZ() - center.getZ(), r)) return false;
                }
                if (cfg.bientityCondition.isPresent()) {
                    if (!cfg.bientityCondition.get().test(new BiEntityCtx(actor, target, actor.level()))) return false;
                }
                return true;
            });
        for (LivingEntity target : nearby) {
            cfg.bientityAction.run(new BiEntityCtx(actor, target, actor.level()));
        }
    }
}
