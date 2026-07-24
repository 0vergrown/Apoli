package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.Space;
import dev.overgrown.apoli.network.VelocityUpdater;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class AddVelocityAction implements ActionType<EntityCtx, AddVelocityAction.Cfg> {
    public record Cfg(Expression x, Expression y, Expression z, Space space, boolean set) {}

    private static final Expression ZERO = Expression.constant(0.0);

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("x", ZERO).forGetter(Cfg::x),
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("y", ZERO).forGetter(Cfg::y),
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("z", ZERO).forGetter(Cfg::z),
            Space.CODEC.optionalFieldOf("space", Space.WORLD).forGetter(Cfg::space),
            Codec.BOOL.optionalFieldOf("set", false).forGetter(Cfg::set)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity recipient = ctx.entity();
        Vec3 delta = cfg.space.toGlobal(recipient, new Vec3(
            cfg.x.eval(recipient),
            cfg.y.eval(recipient),
            cfg.z.eval(recipient)
        ));
        VelocityUpdater.apply(recipient, delta, cfg.set);
    }
}
