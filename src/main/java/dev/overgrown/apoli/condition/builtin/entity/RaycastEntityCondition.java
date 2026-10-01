package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.FluidHandling;
import dev.overgrown.apoli.data.ShapeType;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.data.Vector;
import dev.overgrown.apoli.dev.DevParticles;
import dev.overgrown.apoli.util.InteractionRange;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class RaycastEntityCondition implements ConditionType<EntityCtx, RaycastEntityCondition.Cfg> {
    public record Cfg(
        Optional<Float> distance,
        boolean block,
        boolean entity,
        ShapeType shapeType,
        FluidHandling fluidHandling,
        Space space,
        Optional<Vector> direction,
        Optional<BiEntityCondition> matchBientityCondition,
        Optional<BiEntityCondition> hitBientityCondition,
        Optional<Float> entityDistance,
        Optional<BlockCondition> blockCondition,
        Optional<Float> blockDistance
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("distance").forGetter(Cfg::distance),
            Codec.BOOL.optionalFieldOf("block", true).forGetter(Cfg::block),
            Codec.BOOL.optionalFieldOf("entity", true).forGetter(Cfg::entity),
            ShapeType.CODEC.optionalFieldOf("shape_type", ShapeType.VISUAL).forGetter(Cfg::shapeType),
            FluidHandling.CODEC.optionalFieldOf("fluid_handling", FluidHandling.ANY).forGetter(Cfg::fluidHandling),
            Space.CODEC.optionalFieldOf("space", Space.WORLD).forGetter(Cfg::space),
            Vector.CODEC.optionalFieldOf("direction").forGetter(Cfg::direction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("match_bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::matchBientityCondition),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("hit_bientity_condition", BiEntityCondition.CODEC).forGetter(Cfg::hitBientityCondition),
            Codec.FLOAT.optionalFieldOf("entity_distance").forGetter(Cfg::entityDistance),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("block_condition", BlockCondition.CODEC).forGetter(Cfg::blockCondition),
            Codec.FLOAT.optionalFieldOf("block_distance").forGetter(Cfg::blockDistance)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity source = ctx.entity();
        Level level = ctx.level();
        Vec3 origin = source.getEyePosition();
        Vec3 dir = cfg.direction.isPresent()
            ? cfg.space.toGlobal(source, new Vec3(cfg.direction.get().x(), cfg.direction.get().y(), cfg.direction.get().z()))
            : source.getViewVector(1f);
        if (dir.lengthSqr() < 1.0e-6) dir = source.getViewVector(1f);
        dir = dir.normalize();
        float blockDist = InteractionRange.block(source, cfg.blockDistance, cfg.distance);
        float entityDist = InteractionRange.entity(source, cfg.entityDistance, cfg.distance);

        BlockHitResult blockHit = null;
        double nearestSq = Double.POSITIVE_INFINITY;
        if (cfg.block) {
            Vec3 to = origin.add(dir.scale(blockDist));
            BlockHitResult hit = level.clip(new ClipContext(origin, to, cfg.shapeType.vanilla(), cfg.fluidHandling.vanilla(), source));
            if (hit.getType() != HitResult.Type.MISS) {
                blockHit = hit;
                nearestSq = origin.distanceToSqr(hit.getLocation());
            }
        }

        Entity entityHit = null;
        if (cfg.entity) {
            Vec3 endE = origin.add(dir.scale(entityDist));
            AABB box = new AABB(origin, endE).inflate(1.0);
            List<Entity> cands = level.getEntities(source, box, e ->
                e != source && e.isPickable());
            for (int i = 0; i < cands.size(); i++) {
                Entity cand = cands.get(i);
                AABB targetBox = cand.getBoundingBox();
                Vec3 hitPos;
                if (targetBox.contains(origin)) {
                    hitPos = origin;
                } else {
                    Optional<Vec3> inter = targetBox.clip(origin, endE);
                    if (inter.isEmpty()) continue;
                    hitPos = inter.get();
                }
                double distSq = origin.distanceToSqr(hitPos);
                if (distSq > nearestSq) continue;
                if (cfg.matchBientityCondition.isPresent()
                    && !cfg.matchBientityCondition.get().test(new BiEntityCtx(source, cand, level))) continue;
                entityHit = cand;
                nearestSq = distSq;
            }
        }

        boolean passed;
        if (entityHit != null) {
            passed = cfg.hitBientityCondition.isEmpty()
                || cfg.hitBientityCondition.get().test(new BiEntityCtx(source, entityHit, level));
        } else if (blockHit != null) {
            passed = cfg.blockCondition.isEmpty() || cfg.blockCondition.get().test(new BlockCtx(blockHit.getBlockPos(),
                level.getBlockState(blockHit.getBlockPos()), level));
        } else {
            passed = false;
        }
        if (DevParticles.due(level, source, cfg)) {
            outline((ServerLevel) level, cfg, origin, dir, blockDist, entityDist, blockHit, entityHit, nearestSq, passed);
        }
        return passed;
    }

    private static void outline(ServerLevel level, Cfg cfg, Vec3 origin, Vec3 dir, float blockDist, float entityDist,
                                @Nullable BlockHitResult blockHit, @Nullable Entity entityHit, double stopSq,
                                boolean passed) {
        DevParticles.Ray kind = DevParticles.Ray.CONDITION;
        double reach = Math.max(cfg.block ? blockDist : 0.0, cfg.entity ? entityDist : 0.0);
        DevParticles.ray(level, kind, origin, dir, Double.isInfinite(stopSq) ? reach : Math.sqrt(stopSq), reach);
        if (cfg.entity) {
            double length = blockHit == null ? entityDist : Math.min(entityDist, origin.distanceTo(blockHit.getLocation()));
            DevParticles.detection(level, kind, origin, dir, length, 0.0, 0.0, 0.0, -1.0);
        }
        if (entityHit != null) {
            DevParticles.mark(level, kind, entityHit, passed);
        } else if (blockHit != null) {
            DevParticles.mark(level, kind, blockHit.getBlockPos(), passed);
        }
    }
}
