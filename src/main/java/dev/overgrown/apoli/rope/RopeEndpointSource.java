package dev.overgrown.apoli.rope;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.compat.sable.SableSubLevels;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.util.StringRepresentable;
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
import java.util.UUID;

public record RopeEndpointSource(Type type, float distance, boolean entities, boolean blocks,
                                 Optional<Vector> position) {

    public enum Type implements StringRepresentable {
        SELF("self"), TARGET("target"), RAYCAST("raycast"), POSITION("position");

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        private final String name;
        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<RopeEndpointSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Type.CODEC.optionalFieldOf("source", Type.SELF).forGetter(RopeEndpointSource::type),
        Codec.FLOAT.optionalFieldOf("distance", 30f).forGetter(RopeEndpointSource::distance),
        Codec.BOOL.optionalFieldOf("entities", false).forGetter(RopeEndpointSource::entities),
        Codec.BOOL.optionalFieldOf("blocks", true).forGetter(RopeEndpointSource::blocks),
        Vector.CODEC.optionalFieldOf("position").forGetter(RopeEndpointSource::position)
    ).apply(i, RopeEndpointSource::new));

    public static RopeEndpointSource self() {
        return new RopeEndpointSource(Type.SELF, 30f, false, true, Optional.empty());
    }

    public static RopeEndpointSource raycastBlock(float distance) {
        return new RopeEndpointSource(Type.RAYCAST, distance, false, true, Optional.empty());
    }

    public @Nullable RopeAnchor resolve(@Nullable Entity actor, @Nullable Entity target, Level level) {
        return switch (type) {
            case SELF -> actor == null ? null : new RopeAnchor.OfEntity(actor.getId(), Vec3.ZERO);
            case TARGET -> target == null ? null : new RopeAnchor.OfEntity(target.getId(), Vec3.ZERO);
            case POSITION -> position.map(v -> (RopeAnchor) new RopeAnchor.Position(new Vec3(v.x(), v.y(), v.z())))
                .orElse(null);
            case RAYCAST -> raycast(actor, level);
        };
    }

    private @Nullable RopeAnchor raycast(@Nullable Entity actor, Level level) {
        if (actor == null) return null;
        Vec3 origin = actor.getEyePosition(1f);
        Vec3 dir = actor.getViewVector(1f);
        Vec3 end = origin.add(dir.scale(distance));

        Vec3 blockHitPos = null;
        UUID blockHitSubLevel = null;
        double blockDistSq = Double.POSITIVE_INFINITY;
        double maxDistSq = (distance + 2.0) * (distance + 2.0);
        if (blocks) {
            BlockHitResult blockHit = level.clip(new ClipContext(origin, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                blockHitPos = blockHit.getLocation();
                blockHitSubLevel = SableSubLevels.subLevelAt(level, blockHitPos);
                Vec3 worldPos = blockHitSubLevel == null
                    ? blockHitPos
                    : SableSubLevels.toWorld(level, blockHitSubLevel, blockHitPos);
                blockDistSq = worldPos == null ? Double.POSITIVE_INFINITY : origin.distanceToSqr(worldPos);
                if (blockDistSq > maxDistSq) {
                    blockHitPos = null;
                    blockHitSubLevel = null;
                    blockDistSq = Double.POSITIVE_INFINITY;
                }
            }
        }

        if (entities) {
            AABB box = new AABB(origin, end).inflate(1.0);
            List<Entity> candidates = level.getEntities(actor, box,
                e -> e != actor && e instanceof LivingEntity && e.isPickable());
            Entity best = null;
            double bestSq = blockDistSq;
            for (Entity cand : candidates) {
                Optional<Vec3> inter = cand.getBoundingBox().clip(origin, end);
                if (inter.isEmpty()) continue;
                double dSq = origin.distanceToSqr(inter.get());
                if (dSq < bestSq) { bestSq = dSq; best = cand; }
            }
            if (best != null) return new RopeAnchor.OfEntity(best.getId(), Vec3.ZERO);
        }

        if (blockHitPos == null) return null;
        return blockHitSubLevel == null
            ? new RopeAnchor.Position(blockHitPos)
            : new RopeAnchor.OfSubLevel(blockHitSubLevel, blockHitPos);
    }
}
