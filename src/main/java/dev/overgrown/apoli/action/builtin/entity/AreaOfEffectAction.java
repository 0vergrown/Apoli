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
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AreaOfEffectAction implements ActionType<EntityCtx, AreaOfEffectAction.Cfg> {
    public record Cfg(
        Vector radius,
        Shape shape,
        Optional<BiEntityAction> bientityAction,
        Optional<BiEntityCondition> bientityCondition,
        boolean includeActor,
        Optional<dev.overgrown.apoli.action.EntityAction> afterAction
    ) {}

    private static final int SLOT_COUNT = dev.overgrown.apoli.data.expr.ExprContext.slot("count");
    private static final int SLOT_INDEX = dev.overgrown.apoli.data.expr.ExprContext.slot("index");

    private static final MapCodec<Cfg> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
        Vector.SCALAR_OR_VECTOR.optionalFieldOf("radius", Vector.uniform(16.0f)).forGetter(Cfg::radius),
        Shape.CODEC.optionalFieldOf("shape", Shape.CUBE).forGetter(Cfg::shape),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Cfg::bientityAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::bientityCondition),
        Codec.BOOL.optionalFieldOf("include_actor", false).forGetter(Cfg::includeActor),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("after_action", dev.overgrown.apoli.action.EntityAction.CODEC).forGetter(Cfg::afterAction)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(INNER, Map.of("include_target", "include_actor"));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity actor = ctx.entity();
        BlockPos center = actor.blockPosition();
        int rx = (int) Math.ceil(cfg.radius.x());
        int ry = (int) Math.ceil(cfg.radius.y());
        int rz = (int) Math.ceil(cfg.radius.z());
        int boxHalf = Math.max(rx, Math.max(ry, rz));
        AABB box = AABB.ofSize(actor.position(), boxHalf * 2.0, boxHalf * 2.0, boxHalf * 2.0);
        List<Entity> nearby = actor.level().getEntities((Entity) null, box,
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
        int count = nearby.size();
        if (cfg.bientityAction.isPresent()) {
            BiEntityAction action = cfg.bientityAction.get();
            double previousCount = dev.overgrown.apoli.data.expr.ExprContext.push(SLOT_COUNT, count);
            double previousIndex = dev.overgrown.apoli.data.expr.ExprContext.get(SLOT_INDEX);
            try {
                for (int i = 0; i < count; i++) {
                    dev.overgrown.apoli.data.expr.ExprContext.push(SLOT_INDEX, i);
                    action.run(BiEntityCtx.of(actor, nearby.get(i), actor.level()));
                }
            } finally {
                dev.overgrown.apoli.data.expr.ExprContext.pop(SLOT_INDEX, previousIndex);
                dev.overgrown.apoli.data.expr.ExprContext.pop(SLOT_COUNT, previousCount);
            }
        }
        if (cfg.afterAction.isPresent()) {
            double previousCount = dev.overgrown.apoli.data.expr.ExprContext.push(SLOT_COUNT, count);
            try {
                cfg.afterAction.get().run(ctx);
            } finally {
                dev.overgrown.apoli.data.expr.ExprContext.pop(SLOT_COUNT, previousCount);
            }
        }
    }
}
