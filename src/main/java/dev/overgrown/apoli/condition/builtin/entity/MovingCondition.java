package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.phys.Vec3;

public final class MovingCondition implements ConditionType<EntityCtx, MovingCondition.Cfg> {
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
        Vec3 v = ctx.entity().getDeltaMovement();
        boolean h = v.x * v.x + v.z * v.z > 1.0e-6;
        boolean ve = Math.abs(v.y) > 1.0e-3;
        return (cfg.horizontally && h) || (cfg.vertically && ve);
    }
}
