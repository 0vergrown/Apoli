package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.access.KnownMovement;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class MovingCondition implements ConditionType<EntityCtx, MovingCondition.Cfg> {
    private static final long STALE_TICKS = 2;

    public record Cfg(boolean horizontally, boolean vertically) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("horizontally", true).forGetter(Cfg::horizontally),
            Codec.BOOL.optionalFieldOf("vertically", true).forGetter(Cfg::vertically)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        double dx;
        double dy;
        double dz;
        if (entity.level().isClientSide() && !entity.isControlledByLocalInstance()) {
            dx = entity.getX() - entity.xo;
            dy = entity.getY() - entity.yo;
            dz = entity.getZ() - entity.zo;
        } else if (!entity.level().isClientSide() && entity instanceof ServerPlayer && entity instanceof KnownMovement known
            && !entity.isPassenger()) {
            boolean fresh = entity.level().getGameTime() - known.apoli$knownMovementTime() <= STALE_TICKS;
            dx = fresh ? known.apoli$knownX() : 0;
            dy = fresh ? known.apoli$knownY() : 0;
            dz = fresh ? known.apoli$knownZ() : 0;
        } else {
            Vec3 v = entity.getDeltaMovement();
            dx = v.x;
            dy = v.y;
            dz = v.z;
        }
        boolean h = dx * dx + dz * dz > 1.0e-6;
        boolean ve = Math.abs(dy) > 1.0e-3;
        return (cfg.horizontally && h) || (cfg.vertically && ve);
    }
}
