package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.Shape;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockInRadiusCondition implements ConditionType<EntityCtx, BlockInRadiusCondition.Cfg> {
    public record Cfg(BlockCondition blockCondition, Vector radius, Shape shape, Expression hollowThickness,
                      Comparison comparison, Expression compareTo) {}

    private record OffsetKey(int rx, int ry, int rz, Shape shape, double thickness) {}

    private static final long MAX_SORTED_VOLUME = 35937L;
    private static final int MAX_CACHED_SHAPES = 256;
    private static final Map<OffsetKey, int[]> OFFSETS = new ConcurrentHashMap<>();

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.fieldOf("block_condition").forGetter(Cfg::blockCondition),
            Vector.SCALAR_OR_VECTOR.fieldOf("radius").forGetter(Cfg::radius),
            Shape.CODEC.optionalFieldOf("shape", Shape.CUBE).forGetter(Cfg::shape),
            Shape.HOLLOW_THICKNESS.forGetter(Cfg::hollowThickness),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison),
            Expression.INT_OR_EXPR.optionalFieldOf("compare_to", Expression.constant(1)).forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        BlockPos center = ctx.entity().blockPosition();
        int rx = (int) Math.ceil(cfg.radius.x());
        int ry = (int) Math.ceil(cfg.radius.y());
        int rz = (int) Math.ceil(cfg.radius.z());

        if (dev.overgrown.apoli.dev.DevParticles.due(ctx.level(), ctx.entity(), cfg)
            && ctx.level() instanceof net.minecraft.server.level.ServerLevel devLevel) {
            dev.overgrown.apoli.dev.DevParticles.outlineCondition(devLevel,
                net.minecraft.world.phys.Vec3.atCenterOf(center), cfg.shape,
                cfg.radius.x(), cfg.radius.y(), cfg.radius.z());
        }

        int threshold = cfg.compareTo.evalInt(ctx.entity());
        long limit = (long) threshold + 1L;
        Level level = ctx.level();
        Shape shape = cfg.shape;
        double thickness = shape.thickness(cfg.hollowThickness, ctx.entity());
        BlockCondition condition = cfg.blockCondition;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int baseX = center.getX(), baseY = center.getY(), baseZ = center.getZ();
        int count = 0;

        if ((2L * rx + 1) * (2L * ry + 1) * (2L * rz + 1) <= MAX_SORTED_VOLUME) {
            int[] offsets = offsets(rx, ry, rz, shape, thickness);
            for (int i = 0; i < offsets.length; i += 3) {
                cursor.set(baseX + offsets[i], baseY + offsets[i + 1], baseZ + offsets[i + 2]);
                if (!condition.test(new BlockCtx(cursor.immutable(), level.getBlockState(cursor), level))) continue;
                if (++count >= limit) return cfg.comparison.compare(count, threshold);
            }
            return cfg.comparison.compare(count, threshold);
        }

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    if (!shape.contains(dx, dy, dz, rx, ry, rz, thickness)) continue;
                    cursor.set(baseX + dx, baseY + dy, baseZ + dz);
                    if (!condition.test(new BlockCtx(cursor.immutable(), level.getBlockState(cursor), level))) continue;
                    if (++count >= limit) return cfg.comparison.compare(count, threshold);
                }
            }
        }
        return cfg.comparison.compare(count, threshold);
    }

    private static int[] offsets(int rx, int ry, int rz, Shape shape, double thickness) {
        OffsetKey key = new OffsetKey(rx, ry, rz, shape, thickness);
        int[] cached = OFFSETS.get(key);
        if (cached != null) return cached;
        int[] built = nearestFirst(rx, ry, rz, shape, thickness);
        if (OFFSETS.size() < MAX_CACHED_SHAPES) OFFSETS.putIfAbsent(key, built);
        return built;
    }

    private static int[] nearestFirst(int rx, int ry, int rz, Shape shape, double thickness) {
        int[] raw = new int[(2 * rx + 1) * (2 * ry + 1) * (2 * rz + 1) * 3];
        int size = 0;
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    if (!shape.contains(dx, dy, dz, rx, ry, rz, thickness)) continue;
                    raw[size++] = dx;
                    raw[size++] = dy;
                    raw[size++] = dz;
                }
            }
        }
        int points = size / 3;
        Integer[] order = new Integer[points];
        for (int i = 0; i < points; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingInt(i -> {
            int b = i * 3;
            return raw[b] * raw[b] + raw[b + 1] * raw[b + 1] + raw[b + 2] * raw[b + 2];
        }));
        int[] out = new int[size];
        for (int i = 0; i < points; i++) {
            int from = order[i] * 3;
            out[i * 3] = raw[from];
            out[i * 3 + 1] = raw[from + 1];
            out[i * 3 + 2] = raw[from + 2];
        }
        return out;
    }
}
