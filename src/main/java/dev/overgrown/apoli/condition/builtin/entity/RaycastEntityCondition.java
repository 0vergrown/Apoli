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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
            BiEntityCondition.CODEC.optionalFieldOf("match_bientity_condition").forGetter(Cfg::matchBientityCondition),
            BiEntityCondition.CODEC.optionalFieldOf("hit_bientity_condition").forGetter(Cfg::hitBientityCondition),
            Codec.FLOAT.optionalFieldOf("entity_distance").forGetter(Cfg::entityDistance),
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Cfg::blockCondition),
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
        float blockDist = cfg.blockDistance.orElseGet(() -> cfg.distance.orElse(20f));
        float entityDist = cfg.entityDistance.orElseGet(() -> cfg.distance.orElse(blockDist));

        BlockHitResult blockHit = null;
        if (cfg.block) {
            Vec3 to = origin.add(dir.scale(blockDist));
            blockHit = level.clip(new ClipContext(origin, to, cfg.shapeType.vanilla(), cfg.fluidHandling.vanilla(), source));
            if (blockHit.getType() == HitResult.Type.MISS) blockHit = null;
            if (blockHit != null && cfg.blockCondition.isPresent()) {
                if (!cfg.blockCondition.get().test(new BlockCtx(blockHit.getBlockPos(),
                    level.getBlockState(blockHit.getBlockPos()), level))) blockHit = null;
            }
        }

        if (cfg.entity) {
            Vec3 endE = origin.add(dir.scale(entityDist));
            AABB box = new AABB(origin, endE).inflate(1.0);
            List<Entity> cands = level.getEntities(source, box, e ->
                e != source && e.isPickable());
            for (Entity cand : cands) {
                Optional<Vec3> inter = cand.getBoundingBox().clip(origin, endE);
                if (inter.isEmpty()) continue;
                if (blockHit != null && origin.distanceToSqr(inter.get()) > origin.distanceToSqr(blockHit.getLocation())) continue;
                BiEntityCtx bctx = new BiEntityCtx(source, cand, level);
                if (cfg.matchBientityCondition.isPresent() && !cfg.matchBientityCondition.get().test(bctx)) continue;
                if (cfg.hitBientityCondition.isPresent() && !cfg.hitBientityCondition.get().test(bctx)) continue;
                return true;
            }
        }
        return blockHit != null;
    }
}
