package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.builtin.entity.RaycastAction;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.FluidHandling;
import dev.overgrown.apoli.data.ShapeType;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.data.Vector;
import dev.overgrown.apoli.util.InteractionRange;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;

public final class RaycastBiEntityCondition implements ConditionType<BiEntityCtx, RaycastBiEntityCondition.Cfg> {
    public record Cfg(
        Optional<Float> distance,
        boolean block,
        ShapeType shapeType,
        FluidHandling fluidHandling,
        Space space,
        Optional<Vector> direction,
        Optional<Vector> radius,
        Optional<Float> coneAngle
    ) {}

    private static final MapCodec<Cfg> CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("distance").forGetter(Cfg::distance),
            Codec.BOOL.optionalFieldOf("block", true).forGetter(Cfg::block),
            ShapeType.CODEC.optionalFieldOf("shape_type", ShapeType.VISUAL).forGetter(Cfg::shapeType),
            FluidHandling.CODEC.optionalFieldOf("fluid_handling", FluidHandling.ANY).forGetter(Cfg::fluidHandling),
            Space.CODEC.optionalFieldOf("space", Space.WORLD).forGetter(Cfg::space),
            Vector.CODEC.optionalFieldOf("direction").forGetter(Cfg::direction),
            Vector.SCALAR_OR_VECTOR.optionalFieldOf("radius").forGetter(Cfg::radius),
            Codec.FLOAT.optionalFieldOf("cone_angle").forGetter(Cfg::coneAngle)
        ).apply(i, Cfg::new)),
        Map.of("entity_distance", "distance")
    );

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public boolean test(Cfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity target = ctx.target();
        if (actor == null || target == null || actor == target || actor.level() != target.level()) return false;
        Vec3 origin = actor.getEyePosition();
        Vec3 dir = cfg.direction.isPresent()
            ? cfg.space.toGlobal(actor, new Vec3(cfg.direction.get().x(), cfg.direction.get().y(), cfg.direction.get().z()))
            : actor.getViewVector(1f);
        if (dir.lengthSqr() < 1.0e-6) dir = actor.getViewVector(1f);
        dir = dir.normalize();
        double range = cfg.distance.isPresent() ? cfg.distance.get() : InteractionRange.entity(actor);
        double rx = 0.0;
        double ry = 0.0;
        double rz = 0.0;
        if (cfg.radius.isPresent()) {
            Vector radius = cfg.radius.get();
            rx = radius.x();
            ry = radius.y();
            rz = radius.z();
        }
        boolean cone = cfg.coneAngle.isPresent();
        double coneCos = cone ? Math.cos(Math.toRadians(cfg.coneAngle.get())) : -1.0;
        Vec3 hit = RaycastAction.entityHit(target, origin, dir, origin.add(dir.scale(range)), range,
            rx, ry, rz, cone, coneCos);
        if (hit == null) return false;
        if (!cfg.block || origin.distanceToSqr(hit) < 1.0e-8) return true;
        return actor.level().clip(new ClipContext(origin, hit, cfg.shapeType.vanilla(),
            cfg.fluidHandling.vanilla(), actor)).getType() == HitResult.Type.MISS;
    }
}
