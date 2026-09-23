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

public final class BlockInRadiusCondition implements ConditionType<EntityCtx, BlockInRadiusCondition.Cfg> {
    public record Cfg(BlockCondition blockCondition, Vector radius, Shape shape, Comparison comparison, Expression compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.fieldOf("block_condition").forGetter(Cfg::blockCondition),
            Vector.SCALAR_OR_VECTOR.fieldOf("radius").forGetter(Cfg::radius),
            Shape.CODEC.optionalFieldOf("shape", Shape.CUBE).forGetter(Cfg::shape),
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

        if (dev.overgrown.apoli.dev.DevParticles.due(ctx.level())
            && ctx.level() instanceof net.minecraft.server.level.ServerLevel devLevel) {
            dev.overgrown.apoli.dev.DevParticles.outlineCondition(devLevel,
                net.minecraft.world.phys.Vec3.atCenterOf(center), cfg.shape,
                cfg.radius.x(), cfg.radius.y(), cfg.radius.z());
        }

        int threshold = cfg.compareTo.evalInt(ctx.entity());
        long limit = (long) threshold + 1L;
        Level level = ctx.level();
        Shape shape = cfg.shape;
        BlockCondition condition = cfg.blockCondition;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int baseX = center.getX(), baseY = center.getY(), baseZ = center.getZ();
        int count = 0;

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    if (!shape.contains(dx, dy, dz, rx, ry, rz)) continue;
                    cursor.set(baseX + dx, baseY + dy, baseZ + dz);
                    if (!condition.test(new BlockCtx(cursor.immutable(), level.getBlockState(cursor), level))) continue;
                    if (++count >= limit) return cfg.comparison.compare(count, threshold);
                }
            }
        }
        return cfg.comparison.compare(count, threshold);
    }
}
